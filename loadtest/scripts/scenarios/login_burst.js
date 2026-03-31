import { SharedArray } from 'k6/data';
import {
  SLEEP_MAX_MS,
  SLEEP_MIN_MS,
  assertApiOk,
  buildDefaultOptions,
  getResultData,
  jsonRequest,
  sleepBetween,
} from '../lib/common.js';

const ACCOUNTS_FILE = '/work/data/user_accounts.json';

const ACCOUNTS = new SharedArray('accounts', () => {
  const raw = JSON.parse(open(ACCOUNTS_FILE));
  if (!Array.isArray(raw) || raw.length === 0) {
    throw new Error(`accounts file is empty: ${ACCOUNTS_FILE}`);
  }

  return raw.filter((item) => item && item.username && item.password);
});

export const options = buildDefaultOptions('login_burst', '20s:10,1m:30,20s:0');

function pickAccount() {
  const index = (__ITER + __VU - 1) % ACCOUNTS.length;
  return ACCOUNTS[index];
}

export default function () {
  const account = pickAccount();
  const response = jsonRequest(
    'POST',
    '/users/login',
    {
      username: account.username,
      password: account.password,
    },
    { tags: { endpoint: 'users_login' } },
  );

  assertApiOk(response, 'user login');

  const data = getResultData(response) || {};
  if (!data.token) {
    throw new Error(`login response missing token for username=${account.username}`);
  }

  sleepBetween(SLEEP_MIN_MS, SLEEP_MAX_MS);
}
