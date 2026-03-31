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
const RESULTS_DIR = normalizeResultsDir(__ENV.RESULTS_DIR || '/work/results');
const RESULTS_PREFIX = sanitizeFileToken(__ENV.RESULTS_PREFIX || 'ws-handshake');
const RUN_LABEL = sanitizeFileToken(__ENV.RUN_LABEL || '');
const RUN_TIMESTAMP = formatRunTimestamp(new Date());
const RUN_ID = RUN_LABEL
  ? `${RESULTS_PREFIX}-${RUN_LABEL}-${RUN_TIMESTAMP}`
  : `${RESULTS_PREFIX}-${RUN_TIMESTAMP}`;
const WS_SESSION_SOCKET_ERROR_RATE_MAX = Math.max(
  0,
  Math.min(1, envFloat('WS_SESSION_SOCKET_ERROR_RATE_MAX', 0.05)),
);

const wsHandshakeSuccessRate = new Rate('ws_handshake_success_rate');
const wsSessionSocketErrorRate = new Rate('ws_session_socket_error_rate');
const wsHandshakeDurationMs = new Trend('ws_handshake_duration_ms', true);
const wsHeartbeatAckSessionRate = new Rate('ws_heartbeat_ack_session_rate');
const wsHeartbeatAckLatencyMs = new Trend('ws_heartbeat_ack_latency_ms', true);
const wsHeartbeatAckTotal = new Counter('ws_heartbeat_ack_total');
const thresholds = {
  checks: ['rate>0.99'],
  ws_handshake_success_rate: ['rate>0.99'],
  ws_session_socket_error_rate: [`rate<${WS_SESSION_SOCKET_ERROR_RATE_MAX}`],
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

export function handleSummary(data) {
  const summary = {
    runId: RUN_ID,
    scenario: 'ws_handshake',
    generatedAt: new Date().toISOString(),
    config: buildRunConfig(),
    keyMetrics: buildKeyMetrics(data.metrics || {}),
    rawSummary: data,
  };

  return {
    [`${RESULTS_DIR}/${RUN_ID}.summary.json`]: JSON.stringify(summary, null, 2),
  };
}

export function setup() {
  const response = jsonRequest('GET', '/actuator/health', null, {
    tags: { endpoint: 'actuator_health' },
    timeout: REQUEST_TIMEOUT,
  });
  const responseBody = response.json();
  if (
    !check(response, {
      'actuator health status=200': (res) => res.status === 200,
      'actuator health payload status=UP': () => responseBody && responseBody.status === 'UP',
    })
  ) {
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

  const handshakeOk = response && response.status === 101 && opened;
  wsHandshakeSuccessRate.add(handshakeOk);
  wsSessionSocketErrorRate.add(socketError);

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

function buildRunConfig() {
  return {
    baseUrl: BASE_URL,
    wsBaseUrl: WS_BASE_URL,
    wsPath: WS_PATH,
    requestTimeout: REQUEST_TIMEOUT,
    sessionDurationMs: SESSION_DURATION_MS,
    heartbeatIntervalMs: HEARTBEAT_INTERVAL_MS,
    heartbeatEnabled: HEARTBEAT_ENABLED,
    wsP95ConnectMs: WS_P95_CONNECT_MS,
    wsSessionSocketErrorRateMax: WS_SESSION_SOCKET_ERROR_RATE_MAX,
    startVUs: Math.max(0, envInt('START_VUS', 0)),
    stages: String(__ENV.K6_STAGES || '30s:10,2m:50,30s:0'),
    staticTokenEnabled: Boolean(STATIC_TOKEN),
    accountsFile: STATIC_TOKEN ? null : ACCOUNTS_FILE,
    accountsCount: STATIC_TOKEN ? 0 : ACCOUNTS.length,
    resultsDir: RESULTS_DIR,
    resultsPrefix: RESULTS_PREFIX,
    runLabel: RUN_LABEL || null,
  };
}

function buildKeyMetrics(metrics) {
  return {
    checksRate: metricValue(metrics, 'checks', 'rate'),
    httpReqDurationP95Ms: metricValue(metrics, 'http_req_duration', 'p(95)'),
    wsHandshakeSuccessRate: metricValue(metrics, 'ws_handshake_success_rate', 'rate'),
    wsSessionSocketErrorRate: metricValue(metrics, 'ws_session_socket_error_rate', 'rate'),
    wsHandshakeDurationAvgMs: metricValue(metrics, 'ws_handshake_duration_ms', 'avg'),
    wsHandshakeDurationP95Ms: metricValue(metrics, 'ws_handshake_duration_ms', 'p(95)'),
    wsHeartbeatAckSessionRate: metricValue(metrics, 'ws_heartbeat_ack_session_rate', 'rate'),
    wsHeartbeatAckLatencyP95Ms: metricValue(metrics, 'ws_heartbeat_ack_latency_ms', 'p(95)'),
    iterations: metricValue(metrics, 'iterations', 'count'),
    vusMax: metricValue(metrics, 'vus_max', 'value'),
    wsSessions: metricValue(metrics, 'ws_sessions', 'count'),
  };
}

function metricValue(metrics, metricName, statName) {
  const metric = metrics[metricName];
  if (!metric || !metric.values) {
    return null;
  }
  const value = metric.values[statName];
  return value === undefined ? null : value;
}

function envFloat(name, fallback) {
  const raw = __ENV[name];
  if (raw === undefined || raw === null || raw === '') {
    return fallback;
  }
  const parsed = parseFloat(raw);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function normalizeResultsDir(value) {
  const raw = String(value || '').trim();
  if (!raw) {
    return '/work/results';
  }
  return raw.replace(/\/+$/, '');
}

function sanitizeFileToken(value) {
  const raw = String(value || '').trim();
  if (!raw) {
    return '';
  }
  return raw.replace(/[^0-9A-Za-z._-]+/g, '-').replace(/-+/g, '-').replace(/^-|-$/g, '');
}

function formatRunTimestamp(date) {
  const year = date.getFullYear();
  const month = pad2(date.getMonth() + 1);
  const day = pad2(date.getDate());
  const hour = pad2(date.getHours());
  const minute = pad2(date.getMinutes());
  const second = pad2(date.getSeconds());
  return `${year}${month}${day}-${hour}${minute}${second}`;
}

function pad2(value) {
  return value < 10 ? `0${value}` : String(value);
}
