import { fail } from 'k6';
import { SharedArray } from 'k6/data';
import {
  BASE_URL,
  LIST_PAGE_SIZE,
  RANDOM_PAGE_MAX,
  SLEEP_MAX_MS,
  SLEEP_MIN_MS,
  assertApiOk,
  authHeaders,
  buildDefaultOptions,
  extractVideoIds,
  getResultData,
  jsonRequest,
  pickOne,
  randomInt,
  sleepBetween,
} from '../lib/common.js';

const ACCOUNTS_FILE = '/work/data/user_accounts.json';

const ACCOUNTS = new SharedArray('accounts', () => {
  const raw = JSON.parse(open(ACCOUNTS_FILE));
  if (!Array.isArray(raw) || raw.length === 0) {
    throw new Error(`accounts file is empty: ${ACCOUNTS_FILE}`);
  }

  return raw.filter((item) => item && item.uid && item.username && item.password);
});

let currentSession = null;

export const options = buildDefaultOptions('authenticated_mix', '30s:5,1m:15,30s:0');

export function setup() {
  const response = jsonRequest('GET', `/videos?pageNo=1&pageSize=${LIST_PAGE_SIZE}`, null, {
    tags: { endpoint: 'videos_list' },
  });
  if (!assertApiOk(response, 'setup list videos')) {
    fail(`failed to fetch baseline videos from ${BASE_URL}/videos`);
  }

  const pageData = getResultData(response);
  const videoIds = extractVideoIds(pageData);
  if (videoIds.length === 0) {
    fail('no public videos found, run seed_baseline_data.py first');
  }

  const userIds = ACCOUNTS.map((item) => String(item.uid));
  return { videoIds, userIds };
}

function authParams(token, endpoint) {
  return {
    headers: authHeaders(token),
    tags: { endpoint },
  };
}

function ensureLoggedIn() {
  if (currentSession !== null) {
    return currentSession;
  }

  const account = ACCOUNTS[(__VU - 1) % ACCOUNTS.length];
  const response = jsonRequest(
    'POST',
    '/users/login',
    {
      username: account.username,
      password: account.password,
    },
    { tags: { endpoint: 'users_login' } },
  );

  if (!assertApiOk(response, 'user login')) {
    fail(`login failed for username=${account.username}`);
  }

  const data = getResultData(response) || {};
  if (!data.token) {
    fail(`login response missing token for username=${account.username}`);
  }

  currentSession = {
    uid: String(data.uid || account.uid),
    token: data.token,
  };
  return currentSession;
}

function listVideos(token) {
  const response = jsonRequest(
    'GET',
    `/videos?pageNo=${randomInt(1, RANDOM_PAGE_MAX)}&pageSize=${LIST_PAGE_SIZE}`,
    null,
    authParams(token, 'videos_list'),
  );
  assertApiOk(response, 'list videos');
  return extractVideoIds(getResultData(response));
}

function getVideoDetail(token, videoId) {
  const response = jsonRequest('GET', `/videos/${videoId}`, null, authParams(token, 'video_detail'));
  assertApiOk(response, 'video detail');
}

function addVideoView(token, videoId) {
  const response = jsonRequest('POST', `/videos/${videoId}/views`, null, authParams(token, 'video_views'));
  assertApiOk(response, 'video views');
}

function getMyPublicProfile(session) {
  const response = jsonRequest('GET', `/users/${session.uid}`, null, authParams(session.token, 'user_profile'));
  assertApiOk(response, 'user profile');
}

function likeThenUnlike(token, videoId) {
  const likeResponse = jsonRequest('POST', `/me/videos/${videoId}/likes`, null, authParams(token, 'video_like'));
  if (!assertApiOk(likeResponse, 'video like')) {
    return;
  }

  const unlikeResponse = jsonRequest('DELETE', `/me/videos/${videoId}/likes`, null, authParams(token, 'video_unlike'));
  assertApiOk(unlikeResponse, 'video unlike');
}

function followThenUnfollow(session, targetUid) {
  if (!targetUid || String(targetUid) === String(session.uid)) {
    return;
  }

  const followResponse = jsonRequest('POST', `/me/followings/${targetUid}`, null, authParams(session.token, 'user_follow'));
  if (!assertApiOk(followResponse, 'user follow')) {
    return;
  }

  const unfollowResponse = jsonRequest(
    'DELETE',
    `/me/followings/${targetUid}`,
    null,
    authParams(session.token, 'user_unfollow'),
  );
  assertApiOk(unfollowResponse, 'user unfollow');
}

export default function (data) {
  const session = ensureLoggedIn();
  let candidateIds = data.videoIds;
  const dice = Math.random();

  if (dice < 0.30) {
    const refreshedIds = listVideos(session.token);
    if (refreshedIds.length > 0) {
      candidateIds = refreshedIds;
    }
  } else if (dice < 0.55) {
    getVideoDetail(session.token, pickOne(candidateIds));
  } else if (dice < 0.70) {
    addVideoView(session.token, pickOne(candidateIds));
  } else if (dice < 0.85) {
    likeThenUnlike(session.token, pickOne(candidateIds));
  } else if (dice < 0.95) {
    getMyPublicProfile(session);
  } else {
    const otherUsers = data.userIds.filter((uid) => uid !== session.uid);
    followThenUnfollow(session, pickOne(otherUsers));
  }

  sleepBetween(SLEEP_MIN_MS, SLEEP_MAX_MS);
}
