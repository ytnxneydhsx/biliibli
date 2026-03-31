import { check, sleep } from 'k6';
import http from 'k6/http';

const BASE_URL = String(__ENV.BASE_URL || 'http://127.0.0.1:8080').replace(/\/+$/, '');

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  const response = http.get(`${BASE_URL}/health`);

  check(response, {
    'status is 200': (res) => res.status === 200,
  });

  sleep(1);
}
