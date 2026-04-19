import http from 'k6/http';
import ws from 'k6/ws';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const WS_URL = (__ENV.WS_URL || BASE_URL).replace(/^http/, 'ws');
const RATE = Number(__ENV.RATE || 500);
const VUS = Number(__ENV.VUS || 50);
const DURATION_SECONDS = Number(__ENV.DURATION_SECONDS || 30);
const ACCEPT_TIMEOUT_MS = Number(__ENV.ACCEPT_TIMEOUT_MS || 10000);
const PASSWORD = __ENV.TEST_PASSWORD || 'K6test123456';
const DEBUG_ERRORS = String(__ENV.DEBUG_ERRORS || 'false').toLowerCase() === 'true';
const DEBUG_ERROR_LIMIT = Number(__ENV.DEBUG_ERROR_LIMIT || 10);

const perVuIntervalMs = Math.max(1, Math.round((1000 * VUS) / RATE));
const messagesPerVu = Math.max(1, Math.floor((RATE * DURATION_SECONDS) / VUS));
const maxDurationSeconds = Math.ceil(DURATION_SECONDS + ACCEPT_TIMEOUT_MS / 1000 + 30);

export const options = {
  scenarios: {
    im_ws_constant_rate: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: `${maxDurationSeconds}s`,
    },
  },
  thresholds: {
    im_ws_accepted_rate: ['rate>0.99'],
    im_ws_accepted_latency: ['p(95)<1000', 'p(99)<3000'],
  },
};

const sent = new Counter('im_ws_sent');
const accepted = new Counter('im_ws_accepted');
const acceptedTimeout = new Counter('im_ws_accepted_timeout');
const messageReceived = new Counter('im_ws_message_received');
const appError = new Counter('im_ws_app_error');
const unmatchedAccepted = new Counter('im_ws_unmatched_accepted');
const wsError = new Counter('im_ws_error');
const acceptedRate = new Rate('im_ws_accepted_rate');
const acceptedLatency = new Trend('im_ws_accepted_latency', true);

function jsonHeaders() {
  return { headers: { 'Content-Type': 'application/json' } };
}

function postJson(path, body) {
  return http.post(`${BASE_URL}${path}`, JSON.stringify(body), jsonHeaders());
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
  const runId = `${Date.now()}_${Math.floor(Math.random() * 100000)}`;
  const authByVu = [];
  for (let vu = 1; vu <= VUS; vu += 1) {
    authByVu.push(createAuthPair(runId, String(vu)));
  }
  return {
    authByVu,
    runSeed: Date.now() % 1000000000,
    rate: RATE,
    vus: VUS,
    durationSeconds: DURATION_SECONDS,
    perVuIntervalMs,
    messagesPerVu,
  };
}

export default function (data) {
  const auth = data.authByVu[__VU - 1];
  const pending = {};
  let sentByVu = 0;
  let acceptedByVu = 0;
  let sendingDone = false;
  let closed = false;
  let debugErrors = 0;
  const startedAt = Date.now();
  const firstSendDelayMs = Math.floor((perVuIntervalMs * (__VU - 1)) / VUS);

  const url = `${WS_URL}/ws/im?token=${encodeURIComponent(auth.token)}`;
  const response = ws.connect(url, {}, (socket) => {
    function closeIfDone() {
      if (!closed && sendingDone && acceptedByVu >= sentByVu && Object.keys(pending).length === 0) {
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
      if (sentByVu >= messagesPerVu) {
        sendingDone = true;
        return;
      }
      sentByVu += 1;
      const clientMessageId = data.runSeed * 1000000 + __VU * 100000 + sentByVu;
      pending[String(clientMessageId)] = Date.now();
      sent.add(1);
      socket.send(JSON.stringify({
        type: 'send_message',
        conversationType: 1,
        receiverId: auth.receiverId,
        clientMessageId,
        messageType: 1,
        content: {
          text: `k6-rate-${data.rate}-${__VU}-${sentByVu}`,
        },
      }));
    }

    socket.on('open', () => {
      function startSending() {
        sendOne();
        socket.setInterval(() => {
          if (sentByVu >= messagesPerVu) {
            sendingDone = true;
            closeIfDone();
            return;
          }
          sendOne();
        }, perVuIntervalMs);
      }

      if (firstSendDelayMs <= 0) {
        startSending();
        return;
      }
      socket.setTimeout(startSending, firstSendDelayMs);
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
        acceptedByVu += 1;
        acceptedRate.add(true);
        closeIfDone();
        return;
      }

      if (type === 'message_received') {
        messageReceived.add(1);
        return;
      }

      if (type === 'error' || code !== 0) {
        appError.add(1);
        if (DEBUG_ERRORS && debugErrors < DEBUG_ERROR_LIMIT) {
          debugErrors += 1;
          console.error(`app error vu=${__VU}: ${raw}`);
        }
      }
    });

    socket.on('error', () => {
      wsError.add(1);
    });

    socket.setInterval(() => {
      markTimeouts();
      closeIfDone();
      if (!closed && Date.now() - startedAt > (DURATION_SECONDS * 1000 + ACCEPT_TIMEOUT_MS + 20000)) {
        closed = true;
        socket.close();
      }
    }, 500);
  });

  check(response, {
    'websocket connected': (r) => r && r.status === 101,
  });
}

function createAuthPair(runId, suffix) {
  const senderUsername = `k6_rate_s_${runId}_${suffix}`;
  const receiverUsername = `k6_rate_r_${runId}_${suffix}`;

  const senderRegister = postJson('/users/register', {
    username: senderUsername,
    nickname: 'K6 Rate Sender',
    password: PASSWORD,
    confirmPassword: PASSWORD,
  });
  check(senderRegister, {
    'register sender ok': (r) => r.status === 200 && r.body.includes('"code":0'),
  });

  const receiverRegister = postJson('/users/register', {
    username: receiverUsername,
    nickname: 'K6 Rate Receiver',
    password: PASSWORD,
    confirmPassword: PASSWORD,
  });
  check(receiverRegister, {
    'register receiver ok': (r) => r.status === 200 && r.body.includes('"code":0'),
  });

  const receiverId = extractDataId(receiverRegister.body);
  const login = postJson('/users/login', {
    username: senderUsername,
    password: PASSWORD,
  });
  check(login, {
    'login sender ok': (r) => r.status === 200 && r.body.includes('"code":0'),
  });

  return {
    token: parseToken(login.body),
    receiverId,
  };
}
