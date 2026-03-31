import { fail } from 'k6';
import {
  BASE_URL,
  LIST_PAGE_SIZE,
  RANDOM_PAGE_MAX,
  SLEEP_MAX_MS,
  SLEEP_MIN_MS,
  assertApiOk,
  buildDefaultOptions,
  extractVideoIds,
  getResultData,
  jsonRequest,
  pickOne,
  randomInt,
  sleepBetween,
} from '../lib/common.js';

export const options = buildDefaultOptions('public_browse', '30s:5,1m:20,30s:0');

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

  return { videoIds };
}

function listVideos() {
  const response = jsonRequest(
    'GET',
    `/videos?pageNo=${randomInt(1, RANDOM_PAGE_MAX)}&pageSize=${LIST_PAGE_SIZE}`,
    null,
    { tags: { endpoint: 'videos_list' } },
  );
  assertApiOk(response, 'list videos');
  return extractVideoIds(getResultData(response));
}

function getVideoDetail(videoId) {
  const response = jsonRequest('GET', `/videos/${videoId}`, null, {
    tags: { endpoint: 'video_detail' },
  });
  assertApiOk(response, 'video detail');
}

function getVideoComments(videoId) {
  const response = jsonRequest(
    'GET',
    `/videos/${videoId}/comments?pageNo=1&pageSize=${LIST_PAGE_SIZE}`,
    null,
    { tags: { endpoint: 'video_comments' } },
  );
  assertApiOk(response, 'video comments');
}

function addVideoView(videoId) {
  const response = jsonRequest('POST', `/videos/${videoId}/views`, null, {
    tags: { endpoint: 'video_views' },
  });
  assertApiOk(response, 'video views');
}

export default function (data) {
  let candidateIds = data.videoIds;
  const dice = Math.random();

  if (dice < 0.40) {
    const refreshedIds = listVideos();
    if (refreshedIds.length > 0) {
      candidateIds = refreshedIds;
    }
  } else if (dice < 0.75) {
    getVideoDetail(pickOne(candidateIds));
  } else if (dice < 0.90) {
    getVideoComments(pickOne(candidateIds));
  } else {
    addVideoView(pickOne(candidateIds));
  }

  sleepBetween(SLEEP_MIN_MS, SLEEP_MAX_MS);
}
