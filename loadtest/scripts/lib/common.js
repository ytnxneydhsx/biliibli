import http from 'k6/http';
import { check, sleep } from 'k6';

export const BASE_URL = String(__ENV.BASE_URL || 'http://127.0.0.1:8080').replace(/\/+$/, '');
export const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '10s';
export const LIST_PAGE_SIZE = clamp(envInt('LIST_PAGE_SIZE', 20), 1, 50);
export const RANDOM_PAGE_MAX = Math.max(1, envInt('RANDOM_PAGE_MAX', 20));
export const SLEEP_MIN_MS = Math.max(0, envInt('SLEEP_MIN_MS', 300));
export const SLEEP_MAX_MS = Math.max(SLEEP_MIN_MS, envInt('SLEEP_MAX_MS', 1200));
export const HTTP_P95_MS = Math.max(1, envInt('HTTP_P95_MS', 800));

export function envInt(name, fallback) {
  const raw = __ENV[name];
  if (raw === undefined || raw === null || raw === '') {
    return fallback;
  }
  const parsed = parseInt(raw, 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

export function parseStages(raw, fallback) {
  const source = (raw || fallback || '').trim();
  if (!source) {
    return [{ duration: '30s', target: 1 }];
  }

  return source
    .split(',')
    .map((part) => part.trim())
    .filter((part) => part.length > 0)
    .map((part) => {
      const pair = part.split(':');
      if (pair.length !== 2) {
        throw new Error(`invalid K6_STAGES segment: ${part}`);
      }
      const duration = pair[0].trim();
      const target = parseInt(pair[1].trim(), 10);
      if (!duration || !Number.isFinite(target)) {
        throw new Error(`invalid K6_STAGES segment: ${part}`);
      }
      return { duration, target };
    });
}

export function buildDefaultOptions(name, fallbackStages) {
  return {
    scenarios: {
      [name]: {
        executor: 'ramping-vus',
        startVUs: Math.max(0, envInt('START_VUS', 0)),
        stages: parseStages(__ENV.K6_STAGES, fallbackStages),
        gracefulRampDown: __ENV.GRACEFUL_RAMP_DOWN || '15s',
      },
    },
    thresholds: {
      http_req_failed: ['rate<0.01'],
      http_req_duration: [`p(95)<${HTTP_P95_MS}`],
      checks: ['rate>0.99'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],
  };
}

export function jsonRequest(method, path, payload, params) {
  const normalizedParams = params || {};
  const headers = Object.assign(
    {
      Accept: 'application/json',
    },
    normalizedParams.headers || {},
  );

  let body = null;
  if (payload !== undefined && payload !== null) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(payload);
  }

  return http.request(method, `${BASE_URL}${path}`, body, Object.assign({}, normalizedParams, {
    headers,
    timeout: normalizedParams.timeout || REQUEST_TIMEOUT,
  }));
}

export function tryJson(response) {
  try {
    return response.json();
  } catch (_) {
    return null;
  }
}

export function getResultData(response) {
  const body = tryJson(response);
  if (body === null || body === undefined) {
    return null;
  }
  return body.data;
}

export function assertApiOk(response, label) {
  return check(response, {
    [`${label} status=200`]: (res) => res.status === 200,
    [`${label} code=0`]: (res) => {
      const body = tryJson(res);
      return body !== null && body !== undefined && body.code === 0;
    },
  });
}

export function extractVideoIds(pageData) {
  const records = pageData && Array.isArray(pageData.records) ? pageData.records : [];
  return records
    .map((item) => (item && item.id !== undefined && item.id !== null ? String(item.id) : null))
    .filter((item) => item);
}

export function pickOne(items) {
  if (!items || items.length === 0) {
    return null;
  }
  return items[Math.floor(Math.random() * items.length)];
}

export function randomInt(min, max) {
  const low = Math.min(min, max);
  const high = Math.max(min, max);
  return Math.floor(Math.random() * (high - low + 1)) + low;
}

export function sleepBetween(minMs, maxMs) {
  sleep(randomInt(minMs, maxMs) / 1000);
}

export function authHeaders(token) {
  return token ? { Authorization: `Bearer ${token}` } : {};
}
