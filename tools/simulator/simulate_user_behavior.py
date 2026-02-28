#!/usr/bin/env python3
"""
Simulate user behaviors against real APIs to fill interaction data.

Focus on data-producing behaviors:
- update profile (nickname/sign)
- upload avatar
- upload video (chunk workflow)
and common interaction behaviors.
"""

import argparse
import concurrent.futures
import csv
import hashlib
import json
import os
import random
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from collections import Counter, defaultdict, deque
from dataclasses import dataclass, field
from pathlib import Path
from typing import Deque, Dict, List, Optional, Set, Tuple


DEFAULT_COMMENT_POOL = [
    "Nice video!",
    "Great work, learned a lot.",
    "This is helpful, thanks.",
    "I like this part.",
    "Cool editing.",
    "Very clear explanation.",
    "Saved to favorites.",
    "Looking forward to more.",
]

DEFAULT_SIGNS = [
    "Keep learning every day.",
    "Code and coffee.",
    "Exploring backend engineering.",
    "Try, fail, iterate.",
    "Record tiny progress.",
    "Love distributed systems.",
]

# Minimal JPEG bytes (header + footer), enough for image upload endpoint.
MINI_JPEG = bytes([
    0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46,
    0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01,
    0x00, 0x01, 0x00, 0x00, 0xFF, 0xD9
])


@dataclass
class UserSession:
    uid: int
    username: str
    password: str
    token: str
    liked_videos: Set[int] = field(default_factory=set)
    followed_users: Set[int] = field(default_factory=set)
    recent_videos: Deque[int] = field(default_factory=lambda: deque(maxlen=20))
    uploaded_videos: Deque[int] = field(default_factory=lambda: deque(maxlen=20))


def http_json(method: str,
              url: str,
              payload: Optional[Dict] = None,
              token: Optional[str] = None,
              timeout: int = 12) -> Tuple[int, Dict]:
    data = None
    headers = {"Accept": "application/json"}
    if payload is not None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"

    req = urllib.request.Request(url, method=method.upper(), data=data, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            body_text = response.read().decode("utf-8", errors="replace")
            body = json.loads(body_text) if body_text else {}
            return response.getcode(), body
    except urllib.error.HTTPError as ex:
        body_text = ex.read().decode("utf-8", errors="replace")
        try:
            body = json.loads(body_text) if body_text else {}
        except json.JSONDecodeError:
            body = {"message": body_text}
        return ex.code, body


def http_multipart(method: str,
                   url: str,
                   files: List[Tuple[str, str, str, bytes]],
                   fields: Optional[Dict[str, str]] = None,
                   token: Optional[str] = None,
                   timeout: int = 20) -> Tuple[int, Dict]:
    boundary = "----SimBoundary" + uuid.uuid4().hex
    body = bytearray()

    for key, value in (fields or {}).items():
        body.extend(f"--{boundary}\r\n".encode("utf-8"))
        body.extend(f'Content-Disposition: form-data; name="{key}"\r\n\r\n'.encode("utf-8"))
        body.extend(str(value).encode("utf-8"))
        body.extend(b"\r\n")

    for field_name, file_name, content_type, content in files:
        body.extend(f"--{boundary}\r\n".encode("utf-8"))
        body.extend(
            f'Content-Disposition: form-data; name="{field_name}"; filename="{file_name}"\r\n'.encode("utf-8")
        )
        body.extend(f"Content-Type: {content_type}\r\n\r\n".encode("utf-8"))
        body.extend(content)
        body.extend(b"\r\n")

    body.extend(f"--{boundary}--\r\n".encode("utf-8"))

    headers = {
        "Accept": "application/json",
        "Content-Type": f"multipart/form-data; boundary={boundary}",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"

    req = urllib.request.Request(url, method=method.upper(), data=bytes(body), headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            body_text = response.read().decode("utf-8", errors="replace")
            body_json = json.loads(body_text) if body_text else {}
            return response.getcode(), body_json
    except urllib.error.HTTPError as ex:
        body_text = ex.read().decode("utf-8", errors="replace")
        try:
            body_json = json.loads(body_text) if body_text else {}
        except json.JSONDecodeError:
            body_json = {"message": body_text}
        return ex.code, body_json


def is_success(status: int, body: Dict) -> bool:
    return status == 200 and isinstance(body, dict) and body.get("code") == 0


def load_accounts(accounts_file: Path) -> List[Dict]:
    if not accounts_file.exists():
        raise FileNotFoundError(f"accounts file not found: {accounts_file}")

    accounts: List[Dict] = []
    with accounts_file.open("r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            uid_raw = row.get("uid") or ""
            username = (row.get("username") or "").strip()
            password = (row.get("password") or "").strip()
            if not uid_raw.isdigit() or not username or not password:
                continue
            accounts.append({
                "uid": int(uid_raw),
                "username": username,
                "password": password,
            })
    return accounts


def login_all(base_url: str, accounts: List[Dict], workers: int) -> List[UserSession]:
    sessions: List[UserSession] = []

    def login_one(item: Dict) -> Optional[UserSession]:
        payload = {"username": item["username"], "password": item["password"]}
        status, body = http_json("POST", f"{base_url}/users/login", payload)
        if not is_success(status, body):
            return None
        data = body.get("data") or {}
        uid = data.get("uid")
        token = data.get("token")
        if uid is None or not token:
            return None
        return UserSession(
            uid=int(uid),
            username=item["username"],
            password=item["password"],
            token=token,
        )

    max_workers = max(1, min(workers, len(accounts)))
    completed = 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as pool:
        futures = [pool.submit(login_one, item) for item in accounts]
        for future in concurrent.futures.as_completed(futures):
            completed += 1
            session = future.result()
            if session is not None:
                sessions.append(session)
            if completed % 100 == 0 or completed == len(accounts):
                print(f"[INFO] login progress {completed}/{len(accounts)}, success={len(sessions)}")
    return sessions


def fetch_videos(base_url: str, max_pages: int, page_size: int) -> List[Dict]:
    videos: List[Dict] = []
    for page_no in range(1, max_pages + 1):
        qs = urllib.parse.urlencode({"pageNo": page_no, "pageSize": page_size})
        status, body = http_json("GET", f"{base_url}/videos?{qs}")
        if not is_success(status, body):
            break
        data = body.get("data") or []
        if not data:
            break
        videos.extend(data)
        if len(data) < page_size:
            break
    return videos


def update_hot_ids(video_ids: List[int]) -> List[int]:
    hot_cut = max(1, int(len(video_ids) * 0.2))
    return video_ids[:hot_cut]


def pick_video_id(all_ids: List[int], hot_ids: List[int]) -> int:
    if hot_ids and random.random() < 0.8:
        return random.choice(hot_ids)
    return random.choice(all_ids)


def action_list_videos(base_url: str, session: UserSession) -> bool:
    page_no = random.randint(1, 30)
    page_size = random.choice([10, 20, 30, 50])
    qs = urllib.parse.urlencode({"pageNo": page_no, "pageSize": page_size})
    status, body = http_json("GET", f"{base_url}/videos?{qs}", token=session.token)
    return is_success(status, body)


def action_video_detail(base_url: str, session: UserSession, all_ids: List[int], hot_ids: List[int]) -> bool:
    video_id = pick_video_id(all_ids, hot_ids)
    status, body = http_json("GET", f"{base_url}/videos/{video_id}", token=session.token)
    if is_success(status, body):
        session.recent_videos.append(video_id)
        return True
    return False


def action_view(base_url: str, session: UserSession, all_ids: List[int], hot_ids: List[int]) -> bool:
    if session.recent_videos and random.random() < 0.7:
        video_id = random.choice(list(session.recent_videos))
    else:
        video_id = pick_video_id(all_ids, hot_ids)
    status, body = http_json("POST", f"{base_url}/videos/{video_id}/views", token=session.token)
    if is_success(status, body):
        session.recent_videos.append(video_id)
        return True
    return False


def action_like_or_unlike(base_url: str, session: UserSession, all_ids: List[int], hot_ids: List[int]) -> bool:
    should_unlike = bool(session.liked_videos) and random.random() < 0.35
    if should_unlike:
        video_id = random.choice(list(session.liked_videos))
        status, body = http_json("DELETE", f"{base_url}/me/videos/{video_id}/likes", token=session.token)
        if is_success(status, body):
            session.liked_videos.discard(video_id)
            return True
        return False

    candidates = [vid for vid in hot_ids if vid not in session.liked_videos]
    if not candidates:
        candidates = [vid for vid in all_ids if vid not in session.liked_videos]
    if not candidates:
        return True

    video_id = random.choice(candidates)
    status, body = http_json("POST", f"{base_url}/me/videos/{video_id}/likes", token=session.token)
    if is_success(status, body):
        session.liked_videos.add(video_id)
        return True
    return False


def action_comment(base_url: str, session: UserSession, all_ids: List[int], hot_ids: List[int]) -> bool:
    video_id = pick_video_id(all_ids, hot_ids)
    payload = {"content": random.choice(DEFAULT_COMMENT_POOL)}
    status, body = http_json("POST", f"{base_url}/me/videos/{video_id}/comments", payload, token=session.token)
    return is_success(status, body)


def action_follow_or_unfollow(base_url: str, session: UserSession, all_uids: List[int]) -> bool:
    candidates = [uid for uid in all_uids if uid != session.uid]
    if not candidates:
        return True

    should_unfollow = bool(session.followed_users) and random.random() < 0.35
    if should_unfollow:
        target_uid = random.choice(list(session.followed_users))
        status, body = http_json("DELETE", f"{base_url}/me/followings/{target_uid}", token=session.token)
        if is_success(status, body):
            session.followed_users.discard(target_uid)
            return True
        return False

    follow_candidates = [uid for uid in candidates if uid not in session.followed_users]
    if not follow_candidates:
        return True
    target_uid = random.choice(follow_candidates)
    status, body = http_json("POST", f"{base_url}/me/followings/{target_uid}", token=session.token)
    if is_success(status, body):
        session.followed_users.add(target_uid)
        return True
    return False


def action_update_profile(base_url: str, session: UserSession) -> bool:
    payload = {
        "nickname": f"Sim_{session.uid % 100000}_{random.randint(100, 999)}",
        "sign": random.choice(DEFAULT_SIGNS),
    }
    status, body = http_json("PUT", f"{base_url}/me/profile", payload, token=session.token)
    return is_success(status, body)


def action_upload_avatar(base_url: str, session: UserSession) -> bool:
    # Add random bytes in the middle to avoid dedupe patterns.
    random_bytes = os.urandom(64)
    avatar_bytes = MINI_JPEG[:10] + random_bytes + MINI_JPEG[10:]
    files = [("file", f"avatar_{session.uid}.jpg", "image/jpeg", avatar_bytes)]
    status, body = http_multipart("POST", f"{base_url}/me/avatar", files=files, token=session.token)
    return is_success(status, body)


def action_upload_video(base_url: str,
                        session: UserSession,
                        video_ids: List[int],
                        upload_chunk_size: int,
                        video_min_bytes: int,
                        video_max_bytes: int) -> bool:
    file_size = random.randint(video_min_bytes, video_max_bytes)
    video_bytes = os.urandom(file_size)
    md5_text = hashlib.md5(video_bytes).hexdigest()
    file_name = f"sim_{session.uid}_{int(time.time() * 1000)}.mp4"

    total_chunks = (file_size + upload_chunk_size - 1) // upload_chunk_size
    init_payload = {
        "fileName": file_name,
        "totalSize": file_size,
        "chunkSize": upload_chunk_size,
        "totalChunks": total_chunks,
        "contentType": "video/mp4",
        "fileMd5": md5_text,
    }
    status, body = http_json("POST", f"{base_url}/me/videos/uploads/init-session", init_payload, token=session.token, timeout=20)
    if not is_success(status, body):
        return False

    init_data = body.get("data") or {}
    upload_id = init_data.get("uploadId")
    if not upload_id:
        return False

    for index in range(total_chunks):
        start = index * upload_chunk_size
        end = min(start + upload_chunk_size, file_size)
        chunk = video_bytes[start:end]
        files = [("file", f"{file_name}.part{index}", "video/mp4", chunk)]
        chunk_status, chunk_body = http_multipart(
            "PUT",
            f"{base_url}/me/videos/uploads/{upload_id}/chunks/{index}",
            files=files,
            token=session.token,
            timeout=30,
        )
        if not is_success(chunk_status, chunk_body):
            return False

    complete_payload = {
        "title": f"sim upload {session.uid} {int(time.time())}",
        "description": "auto generated by simulator",
        "coverUrl": f"{base_url}/media/cover/mock/{upload_id}.jpg",
        "duration": random.randint(60, 1200),
    }
    complete_status, complete_body = http_json(
        "POST",
        f"{base_url}/me/videos/uploads/{upload_id}/complete",
        complete_payload,
        token=session.token,
        timeout=25,
    )
    if not is_success(complete_status, complete_body):
        return False

    complete_data = complete_body.get("data") or {}
    video_id = complete_data.get("videoId")
    if isinstance(video_id, int):
        # New uploads become candidate videos for later interactions.
        video_ids.insert(0, video_id)
        session.uploaded_videos.append(video_id)
        session.recent_videos.append(video_id)
    return True


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Simulate user behaviors against APIs.")
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--accounts-file", default="tools/simulator/output/user_accounts.csv")
    parser.add_argument("--duration-seconds", type=int, default=1800)
    parser.add_argument("--interval-ms", type=int, default=250)
    parser.add_argument("--max-video-pages", type=int, default=300)
    parser.add_argument("--page-size", type=int, default=50)
    parser.add_argument("--seed", type=int, default=1234)
    parser.add_argument("--print-every", type=int, default=100)
    parser.add_argument("--workers", type=int, default=8, help="Parallel behavior workers")
    parser.add_argument("--login-workers", type=int, default=20, help="Parallel workers for initial login")

    parser.add_argument("--upload-chunk-size", type=int, default=262144, help="Chunk size in bytes for upload action")
    parser.add_argument("--video-min-bytes", type=int, default=200000)
    parser.add_argument("--video-max-bytes", type=int, default=900000)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    random.seed(args.seed)
    base_url = args.base_url.rstrip("/")

    accounts_file = Path(args.accounts_file)
    accounts = load_accounts(accounts_file)
    if not accounts:
        print("[ERROR] no account loaded from ledger")
        return 1

    sessions = login_all(base_url, accounts, args.login_workers)
    if not sessions:
        print("[ERROR] no valid session after login")
        return 1
    print(f"[INFO] active sessions: {len(sessions)}")

    videos = fetch_videos(base_url, args.max_video_pages, args.page_size)
    video_ids = [int(item["id"]) for item in videos if item.get("id") is not None]
    if not video_ids:
        print("[ERROR] no videos found from APIs, please run seed_baseline_data.py first")
        return 1
    hot_ids = update_hot_ids(video_ids)
    all_uids = [item["uid"] for item in accounts]

    # Weighted actions (search removed):
    # list:23, detail:20, view:20, like/unlike:10, comment:8, follow/unfollow:3,
    # profile_update:7, avatar_upload:5, video_upload:4
    actions = [
        ("list", 23),
        ("detail", 20),
        ("view", 20),
        ("like", 10),
        ("comment", 8),
        ("follow", 3),
        ("profile", 7),
        ("avatar", 5),
        ("upload_video", 4),
    ]
    names = [name for name, _ in actions]
    weights = [weight for _, weight in actions]

    stats = Counter()
    errors = defaultdict(int)
    deadline = time.time() + args.duration_seconds
    sleep_seconds = max(args.interval_ms, 0) / 1000.0
    workers = max(1, min(args.workers, len(sessions)))
    print(f"[INFO] workers: {workers}")

    # Split sessions to reduce race on per-user memory state.
    session_groups: List[List[UserSession]] = [[] for _ in range(workers)]
    for idx, item in enumerate(sessions):
        session_groups[idx % workers].append(item)

    video_lock = threading.Lock()
    stat_lock = threading.Lock()
    progress = {"total": 0}

    def snapshot_video_pool() -> Tuple[List[int], List[int]]:
        with video_lock:
            ids_snapshot = list(video_ids)
            hot_snapshot = update_hot_ids(ids_snapshot)
        return ids_snapshot, hot_snapshot

    def add_uploaded_video(video_id: int) -> None:
        with video_lock:
            video_ids.insert(0, video_id)

    def worker_loop(worker_id: int, worker_sessions: List[UserSession]) -> Tuple[Counter, Counter]:
        local_stats = Counter()
        local_errors = Counter()
        rng = random.Random(args.seed + worker_id * 100003)

        while time.time() < deadline:
            session = rng.choice(worker_sessions)
            action = rng.choices(names, weights=weights, k=1)[0]
            ok = False
            try:
                if action == "list":
                    ok = action_list_videos(base_url, session)
                elif action == "detail":
                    ids, hots = snapshot_video_pool()
                    ok = action_video_detail(base_url, session, ids, hots)
                elif action == "view":
                    ids, hots = snapshot_video_pool()
                    ok = action_view(base_url, session, ids, hots)
                elif action == "like":
                    ids, hots = snapshot_video_pool()
                    ok = action_like_or_unlike(base_url, session, ids, hots)
                elif action == "comment":
                    ids, hots = snapshot_video_pool()
                    ok = action_comment(base_url, session, ids, hots)
                elif action == "follow":
                    ok = action_follow_or_unfollow(base_url, session, all_uids)
                elif action == "profile":
                    ok = action_update_profile(base_url, session)
                elif action == "avatar":
                    ok = action_upload_avatar(base_url, session)
                elif action == "upload_video":
                    ids, _ = snapshot_video_pool()
                    ok = action_upload_video(
                        base_url,
                        session,
                        ids,
                        upload_chunk_size=args.upload_chunk_size,
                        video_min_bytes=args.video_min_bytes,
                        video_max_bytes=args.video_max_bytes,
                    )
                    if ok and session.uploaded_videos:
                        add_uploaded_video(session.uploaded_videos[-1])
            except Exception as ex:  # pragma: no cover - runtime safety
                local_errors[f"{action}:{type(ex).__name__}"] += 1
                ok = False

            local_stats[f"{action}_total"] += 1
            if ok:
                local_stats[f"{action}_ok"] += 1
            else:
                local_stats[f"{action}_fail"] += 1

            with stat_lock:
                progress["total"] += 1
                if progress["total"] % args.print_every == 0:
                    print(f"[INFO] progress events={progress['total']}")

            if sleep_seconds > 0:
                time.sleep(sleep_seconds)

        return local_stats, local_errors

    with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as pool:
        futures = [
            pool.submit(worker_loop, worker_id, group if group else sessions)
            for worker_id, group in enumerate(session_groups)
        ]
        for future in concurrent.futures.as_completed(futures):
            local_stats, local_errors = future.result()
            stats.update(local_stats)
            errors.update(local_errors)

    print("\n[RESULT] Behavior simulation summary")
    for name in names:
        total = stats[f"{name}_total"]
        ok = stats[f"{name}_ok"]
        fail = stats[f"{name}_fail"]
        ratio = (ok / total * 100.0) if total else 0.0
        print(f"- {name:11s} total={total:6d} ok={ok:6d} fail={fail:6d} success={ratio:6.2f}%")

    if errors:
        print("\n[RESULT] top runtime errors")
        for key, count in Counter(errors).most_common(10):
            print(f"- {key}: {count}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
