import ws from 'k6/ws';
import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate, Trend } from 'k6/metrics';
import {
  BASE_URL,
  REQUEST_TIMEOUT,
  assertApiOk,
  envInt,
  getResultData,
  jsonRequest,
  parseStages,
} from '../lib/common.js';

const ACCOUNTS_FILE = '/work/data/ws_accounts.json';
const STATIC_TOKEN = String(__ENV.WS_TOKEN || '').trim();
const WS_PATH = normalizeWsPath(__ENV.WS_PATH || '/ws/im');
const WS_BASE_URL = buildWsBaseUrl(__ENV.WS_BASE_URL || '', BASE_URL, WS_PATH);
const SESSION_DURATION_MS = Math.max(1000, envInt('WS_SESSION_DURATION_MS', 15000));
const HEARTBEAT_INTERVAL_MS = Math.max(0, envInt('WS_HEARTBEAT_INTERVAL_MS', 5000));
const WS_P95_CONNECT_MS = Math.max(1, envInt('WS_P95_CONNECT_MS', 1000));
const HEARTBEAT_ENABLED = HEARTBEAT_INTERVAL_MS > 0;

const wsHandshakeSuccessRate = new Rate('ws_handshake_success_rate');
const wsHandshakeDurationMs = new Trend('ws_handshake_duration_ms', true);
const wsHeartbeatAckSessionRate = new Rate('ws_heartbeat_ack_session_rate');
const wsHeartbeatAckLatencyMs = new Trend('ws_heartbeat_ack_latency_ms', true);
const wsHeartbeatAckTotal = new Counter('ws_heartbeat_ack_total');
const thresholds = {
  checks: ['rate>0.99'],
  ws_handshake_success_rate: ['rate>0.99'],
  ws_handshake_duration_ms: [`p(95)<${WS_P95_CONNECT_MS}`],
};

if (HEARTBEAT_ENABLED) {
  thresholds.ws_heartbeat_ack_session_rate = ['rate>0.95'];
}

const ACCOUNTS = STATIC_TOKEN
  ? []
  : new SharedArray('ws_accounts', () => {
      try {
        const raw = JSON.parse(open(ACCOUNTS_FILE));
        if (!Array.isArray(raw) || raw.length === 0) {
          return [];
        }

        return raw.filter((item) => item && item.username && item.password);
      } catch (_) {
        return [];
      }
    });

let currentSession = null;

export const options = {
  scenarios: {
    ws_handshake: {
      executor: 'ramping-vus',
      startVUs: Math.max(0, envInt('START_VUS', 0)),
      stages: parseStages(__ENV.K6_STAGES, '30s:10,2m:50,30s:0'),
      gracefulRampDown: __ENV.GRACEFUL_RAMP_DOWN || '15s',
    },
  },
  thresholds,
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],
};

export function setup() {
  const response = jsonRequest('GET', '/actuator/health', null, {
    tags: { endpoint: 'actuator_health' },
    timeout: REQUEST_TIMEOUT,
  });
  if (!check(response, { 'actuator health status=200': (res) => res.status === 200 })) {
    throw new Error(`failed to reach ${BASE_URL}/actuator/health`);
  }

  return {
    heartbeatEnabled: HEARTBEAT_ENABLED,
    wsBaseUrl: WS_BASE_URL,
  };
}

function ensureSession() {
  if (STATIC_TOKEN) {
    return {
      uid: 'shared-token',
      token: STATIC_TOKEN,
    };
  }

  if (currentSession !== null) {
    return currentSession;
  }

  if (!ACCOUNTS.length) {
    throw new Error(
      `missing websocket credentials: set WS_TOKEN or provide ${ACCOUNTS_FILE}`,
    );
  }

  const account = ACCOUNTS[(__VU - 1) % ACCOUNTS.length];
  const response = jsonRequest(
    'POST',
    '/users/login',
    {
      username: account.username,
      password: account.password,
    },
    {
      tags: { endpoint: 'users_login_for_ws' },
      timeout: REQUEST_TIMEOUT,
    },
  );

  if (!assertApiOk(response, 'ws user login')) {
    throw new Error(`login failed for username=${account.username}`);
  }

  const data = getResultData(response) || {};
  if (!data.token) {
    throw new Error(`login response missing token for username=${account.username}`);
  }

  currentSession = {
    uid: String(data.uid || account.uid),
    token: data.token,
  };
  return currentSession;
}

export default function (data) {
  const session = ensureSession();
  const wsUrl = buildWsUrl(data.wsBaseUrl, session.token);
  const connectStart = Date.now();

  let opened = false;
  let socketError = false;
  let heartbeatsSent = 0;
  let heartbeatsAcked = 0;
  let pendingHeartbeatAt = 0;

  const response = ws.connect(
    wsUrl,
    {
      tags: {
        scenario: 'ws_handshake',
        endpoint: 'im_ws_handshake',
      },
    },
    (socket) => {
      socket.on('open', () => {
        opened = true;
        wsHandshakeDurationMs.add(Date.now() - connectStart);

        if (data.heartbeatEnabled) {
          sendHeartbeat(socket, {
            markSent: () => {
              pendingHeartbeatAt = Date.now();
              heartbeatsSent += 1;
            },
            hasPendingHeartbeat: () => pendingHeartbeatAt > 0,
          });

          socket.setInterval(() => {
            sendHeartbeat(socket, {
              markSent: () => {
                pendingHeartbeatAt = Date.now();
                heartbeatsSent += 1;
              },
              hasPendingHeartbeat: () => pendingHeartbeatAt > 0,
            });
          }, HEARTBEAT_INTERVAL_MS);
        }

        socket.setTimeout(() => {
          socket.close();
        }, SESSION_DURATION_MS);
      });

      socket.on('message', (raw) => {
        const packet = tryParseJson(raw);
        if (!packet || packet.type !== 'heartbeat_ack') {
          return;
        }

        if (pendingHeartbeatAt > 0) {
          wsHeartbeatAckLatencyMs.add(Date.now() - pendingHeartbeatAt);
          pendingHeartbeatAt = 0;
        }

        heartbeatsAcked += 1;
        wsHeartbeatAckTotal.add(1);
      });

      socket.on('error', () => {
        socketError = true;
      });
    },
  );

  const handshakeOk = response && response.status === 101 && opened && !socketError;
  wsHandshakeSuccessRate.add(handshakeOk);

  if (data.heartbeatEnabled) {
    wsHeartbeatAckSessionRate.add(heartbeatsSent === 0 || heartbeatsAcked > 0);
  }

  check(response, {
    'ws handshake status=101': (res) => res && res.status === 101,
    'ws connection opened': () => opened,
    'ws connection without socket error': () => !socketError,
  });
}

function normalizeWsPath(value) {
  const raw = String(value || '').trim();
  if (!raw) {
    return '/ws/im';
  }
  return raw.startsWith('/') ? raw : `/${raw}`;
}

function buildWsBaseUrl(explicitBaseUrl, httpBaseUrl, wsPath) {
  const explicit = String(explicitBaseUrl || '').trim();
  if (explicit) {
    const match = /^(wss?:\/\/[^/?#]+)(\/[^?#]*)?(\?[^#]*)?$/i.exec(explicit);
    if (!match) {
      throw new Error(`invalid WS_BASE_URL: ${explicit}`);
    }
    const path = match[2] && match[2] !== '/' ? match[2] : wsPath;
    const query = match[3] || '';
    return `${match[1]}${path}${query}`;
  }

  const http = String(httpBaseUrl || '').trim();
  const match = /^(https?):\/\/([^/?#]+)(\/.*)?$/i.exec(http);
  if (!match) {
    throw new Error(`invalid BASE_URL: ${httpBaseUrl}`);
  }

  const protocol = match[1].toLowerCase() === 'https' ? 'wss' : 'ws';
  return `${protocol}://${match[2]}${wsPath}`;
}

function buildWsUrl(baseUrl, token) {
  const separator = String(baseUrl).indexOf('?') >= 0 ? '&' : '?';
  return `${baseUrl}${separator}token=${encodeURIComponent(token)}`;
}

function sendHeartbeat(socket, controls) {
  if (!socket || !controls || controls.hasPendingHeartbeat()) {
    return;
  }
  controls.markSent();
  socket.send(JSON.stringify({ type: 'heartbeat' }));
}

function tryParseJson(raw) {
  if (raw === null || raw === undefined || raw === '') {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch (_) {
    return null;
  }
}
