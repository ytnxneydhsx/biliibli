import http from 'k6/http';
import ws from 'k6/ws';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const WS_URL = (__ENV.WS_URL || BASE_URL).replace(/^http/, 'ws');
const PAIRS = Number(__ENV.PAIRS || 300);
const RATE = Number(__ENV.RATE || 600);
const DURATION_SECONDS = Number(__ENV.DURATION_SECONDS || 30);
const ACCEPT_TIMEOUT_MS = Number(__ENV.ACCEPT_TIMEOUT_MS || 10000);
const RECEIVER_WARMUP_MS = Number(__ENV.RECEIVER_WARMUP_MS || 5000);
const PASSWORD = __ENV.TEST_PASSWORD || 'K6test123456';
const DEBUG_ERRORS = String(__ENV.DEBUG_ERRORS || 'false').toLowerCase() === 'true';
const DEBUG_ERROR_LIMIT = Number(__ENV.DEBUG_ERROR_LIMIT || 10);

const totalVus = PAIRS * 2;
const senderIntervalMs = Math.max(1, Math.round((1000 * PAIRS) / RATE));
const messagesPerSender = Math.max(1, Math.floor((RATE * DURATION_SECONDS) / PAIRS));
const maxDurationSeconds = Math.ceil(
  RECEIVER_WARMUP_MS / 1000 + DURATION_SECONDS + ACCEPT_TIMEOUT_MS / 1000 + 45
);

export const options = {
  scenarios: {
    im_ws_online_pairs_window_cache: {
      executor: 'per-vu-iterations',
      vus: totalVus,
      iterations: 1,
      maxDuration: `${maxDurationSeconds}s`,
    },
  },
  thresholds: {
    im_ws_accepted_rate: ['rate>0.99'],
    im_ws_receiver_received_rate: ['rate>0.99'],
    im_ws_accepted_latency: ['p(95)<1000', 'p(99)<3000'],
  },
};

const sent = new Counter('im_ws_sent');
const accepted = new Counter('im_ws_accepted');
const acceptedTimeout = new Counter('im_ws_accepted_timeout');
const senderMessageReceived = new Counter('im_ws_sender_message_received');
const receiverMessageReceived = new Counter('im_ws_receiver_message_received');
const appError = new Counter('im_ws_app_error');
const unmatchedAccepted = new Counter('im_ws_unmatched_accepted');
const wsError = new Counter('im_ws_error');
const acceptedRate = new Rate('im_ws_accepted_rate');
const receiverReceivedRate = new Rate('im_ws_receiver_received_rate');
const acceptedLatency = new Trend('im_ws_accepted_latency', true);

function jsonHeaders(token) {
  const headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return { headers };
}

function postJson(path, body, token) {
  return http.post(`${BASE_URL}${path}`, JSON.stringify(body), jsonHeaders(token));
}

function extractDataId(rawBody) {
  const match = String(rawBody).match(/"data"\s*:\s*"?(\d+)"?/);
  if (!match) {
    throw new Error(`failed to extract data id from response: ${rawBody}`);
  }
  return match[1];
}

function parseToken(rawBody) {
  const body = JSON.parse(rawBody);
  if (!body || body.code !== 0 || !body.data || !body.data.token) {
    throw new Error(`login failed: ${rawBody}`);
  }
  return body.data.token;
}

export function setup() {
  const runId = `${(Date.now() % 2176782336).toString(36)}${Math.floor(Math.random() * 1679616).toString(36)}`;
  const authPairs = [];
  for (let pair = 1; pair <= PAIRS; pair += 1) {
    authPairs.push(createAuthPair(runId, String(pair)));
  }

  return {
    authPairs,
    runSeed: Date.now() % 1000000000,
    pairs: PAIRS,
    rate: RATE,
    durationSeconds: DURATION_SECONDS,
    senderIntervalMs,
    messagesPerSender,
  };
}

export default function (data) {
  if (__VU <= data.pairs) {
    runReceiver(data.authPairs[__VU - 1], data);
    return;
  }

  const pairIndex = __VU - data.pairs - 1;
  runSender(data.authPairs[pairIndex], data, pairIndex);
}

function runReceiver(auth, data) {
  let received = 0;
  let closed = false;
  let debugErrors = 0;
  const expectedMessages = data.messagesPerSender;
  const closeAfterMs = RECEIVER_WARMUP_MS + DURATION_SECONDS * 1000 + ACCEPT_TIMEOUT_MS + 10000;
  const startedAt = Date.now();
  const url = `${WS_URL}/ws/im?token=${encodeURIComponent(auth.receiverToken)}`;

  const response = ws.connect(url, {}, (socket) => {
    socket.on('message', (raw) => {
      let msg;
      try {
        msg = JSON.parse(raw);
      } catch (e) {
        appError.add(1);
        return;
      }

      if (msg.type === 'message_received') {
        received += 1;
        receiverMessageReceived.add(1);
        receiverReceivedRate.add(true);
        return;
      }

      if (msg.type === 'error' || (msg.code != null && msg.code !== 0)) {
        appError.add(1);
        if (DEBUG_ERRORS && debugErrors < DEBUG_ERROR_LIMIT) {
          debugErrors += 1;
          console.error(`receiver app error vu=${__VU}: ${raw}`);
        }
      }
    });

    socket.on('error', () => {
      wsError.add(1);
    });

    socket.setInterval(() => {
      if (!closed && Date.now() - startedAt > closeAfterMs) {
        while (received < expectedMessages) {
          receiverReceivedRate.add(false);
          received += 1;
        }
        closed = true;
        socket.close();
      }
    }, 500);
  });

  check(response, {
    'receiver websocket connected': (r) => r && r.status === 101,
  });

}

function runSender(auth, data, senderIndex) {
  const pending = {};
  let sentBySender = 0;
  let acceptedBySender = 0;
  let sendingDone = false;
  let closed = false;
  let debugErrors = 0;
  const startedAt = Date.now();
  const firstSendDelayMs = RECEIVER_WARMUP_MS
    + Math.floor((data.senderIntervalMs * senderIndex) / data.pairs);
  const url = `${WS_URL}/ws/im?token=${encodeURIComponent(auth.senderToken)}`;

  const response = ws.connect(url, {}, (socket) => {
    function closeIfDone() {
      if (!closed && sendingDone && acceptedBySender >= sentBySender && Object.keys(pending).length === 0) {
        closed = true;
        socket.close();
      }
    }

    function markTimeouts() {
      const now = Date.now();
      Object.keys(pending).forEach((clientMessageId) => {
        if (now - pending[clientMessageId] >= ACCEPT_TIMEOUT_MS) {
          delete pending[clientMessageId];
          acceptedTimeout.add(1);
          acceptedRate.add(false);
        }
      });
    }

    function sendOne() {
      if (sentBySender >= data.messagesPerSender) {
        sendingDone = true;
        return;
      }
      sentBySender += 1;
      const clientMessageId = data.runSeed * 1000000 + (senderIndex + 1) * 100000 + sentBySender;
      pending[String(clientMessageId)] = Date.now();
      sent.add(1);
      socket.send(JSON.stringify({
        type: 'send_message',
        conversationType: 1,
        receiverId: auth.receiverId,
        clientMessageId,
        messageType: 1,
        content: {
          text: `k6-window-cache-${data.rate}-${senderIndex + 1}-${sentBySender}`,
        },
      }));
    }

    socket.on('open', () => {
      socket.setTimeout(() => {
        sendOne();
        socket.setInterval(() => {
          if (sentBySender >= data.messagesPerSender) {
            sendingDone = true;
            closeIfDone();
            return;
          }
          sendOne();
        }, data.senderIntervalMs);
      }, firstSendDelayMs);
    });

    socket.on('message', (raw) => {
      let msg;
      try {
        msg = JSON.parse(raw);
      } catch (e) {
        appError.add(1);
        return;
      }

      const type = msg.type;
      const code = msg.code;
      const clientMessageId = msg.data && msg.data.clientMessageId != null
        ? String(msg.data.clientMessageId)
        : null;

      if (type === 'send_message_accepted') {
        if (code !== 0) {
          appError.add(1);
          if (clientMessageId && pending[clientMessageId]) {
            delete pending[clientMessageId];
            acceptedRate.add(false);
          }
          return;
        }
        if (!clientMessageId || pending[clientMessageId] == null) {
          unmatchedAccepted.add(1);
          return;
        }
        acceptedLatency.add(Date.now() - pending[clientMessageId]);
        delete pending[clientMessageId];
        accepted.add(1);
        acceptedBySender += 1;
        acceptedRate.add(true);
        closeIfDone();
        return;
      }

      if (type === 'message_received') {
        senderMessageReceived.add(1);
        return;
      }

      if (type === 'error' || code !== 0) {
        appError.add(1);
        if (DEBUG_ERRORS && debugErrors < DEBUG_ERROR_LIMIT) {
          debugErrors += 1;
          console.error(`sender app error vu=${__VU}: ${raw}`);
        }
      }
    });

    socket.on('error', () => {
      wsError.add(1);
    });

    socket.setInterval(() => {
      markTimeouts();
      closeIfDone();
      if (!closed && Date.now() - startedAt > (firstSendDelayMs + DURATION_SECONDS * 1000 + ACCEPT_TIMEOUT_MS + 20000)) {
        closed = true;
        socket.close();
      }
    }, 500);
  });

  check(response, {
    'sender websocket connected': (r) => r && r.status === 101,
  });

}

function createAuthPair(runId, suffix) {
  const compactSuffix = Number(suffix).toString(36);
  const senderUsername = `ks${runId}${compactSuffix}`;
  const receiverUsername = `kr${runId}${compactSuffix}`;

  const senderRegister = postJson('/users/register', {
    username: senderUsername,
    nickname: 'K6 Online Sender',
    password: PASSWORD,
    confirmPassword: PASSWORD,
  });
  check(senderRegister, {
    'register sender ok': (r) => r.status === 200 && r.body.includes('"code":0'),
  });

  const receiverRegister = postJson('/users/register', {
    username: receiverUsername,
    nickname: 'K6 Online Receiver',
    password: PASSWORD,
    confirmPassword: PASSWORD,
  });
  check(receiverRegister, {
    'register receiver ok': (r) => r.status === 200 && r.body.includes('"code":0'),
  });

  const receiverId = extractDataId(receiverRegister.body);
  const senderLogin = postJson('/users/login', {
    username: senderUsername,
    password: PASSWORD,
  });
  check(senderLogin, {
    'login sender ok': (r) => r.status === 200 && r.body.includes('"code":0'),
  });

  const receiverLogin = postJson('/users/login', {
    username: receiverUsername,
    password: PASSWORD,
  });
  check(receiverLogin, {
    'login receiver ok': (r) => r.status === 200 && r.body.includes('"code":0'),
  });

  return {
    senderToken: parseToken(senderLogin.body),
    receiverToken: parseToken(receiverLogin.body),
    receiverId,
  };
}
