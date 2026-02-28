#!/usr/bin/env python3
"""
Seed baseline data for behavior simulation (API-only mode).

1) Register users through real APIs and write account/password ledger.
2) Create baseline videos through upload APIs (no direct SQL insert).
"""

import argparse
import concurrent.futures
import csv
import hashlib
import json
import os
import random
import sys
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path
from typing import Dict, List, Optional, Tuple


VIDEO_KEYWORDS = [
    "music", "dance", "game", "anime", "food", "travel", "tech",
    "study", "vlog", "movie", "sports", "news", "pets", "coding"
]


def http_json(method: str,
              url: str,
              payload: Optional[Dict] = None,
              token: Optional[str] = None,
              timeout: int = 15) -> Tuple[int, Dict]:
    data = None
    headers = {"Accept": "application/json"}
    if payload is not None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(url, method=method.upper(), data=data, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            body_raw = response.read().decode("utf-8", errors="replace")
            body = json.loads(body_raw) if body_raw else {}
            return response.getcode(), body
    except urllib.error.HTTPError as ex:
        body_raw = ex.read().decode("utf-8", errors="replace")
        try:
            body = json.loads(body_raw) if body_raw else {}
        except json.JSONDecodeError:
            body = {"message": body_raw}
        return ex.code, body


def http_multipart(method: str,
                   url: str,
                   files: List[Tuple[str, str, str, bytes]],
                   token: Optional[str] = None,
                   timeout: int = 30) -> Tuple[int, Dict]:
    boundary = "----SeedBoundary" + uuid.uuid4().hex
    body = bytearray()

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

    request = urllib.request.Request(url, method=method.upper(), data=bytes(body), headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            body_raw = response.read().decode("utf-8", errors="replace")
            body = json.loads(body_raw) if body_raw else {}
            return response.getcode(), body
    except urllib.error.HTTPError as ex:
        body_raw = ex.read().decode("utf-8", errors="replace")
        try:
            body = json.loads(body_raw) if body_raw else {}
        except json.JSONDecodeError:
            body = {"message": body_raw}
        return ex.code, body


def is_success(status: int, body: Dict) -> bool:
    return status == 200 and isinstance(body, dict) and body.get("code") == 0


def register_or_login(base_url: str, username: str, nickname: str, password: str) -> Optional[Dict]:
    register_url = f"{base_url}/users/register"
    login_url = f"{base_url}/users/login"

    register_payload = {
        "username": username,
        "nickname": nickname,
        "password": password,
        "confirmPassword": password
    }
    status, body = http_json("POST", register_url, register_payload)
    if not is_success(status, body):
        message = str(body.get("message", ""))
        if "already exists" not in message:
            print(f"[WARN] register failed username={username} status={status} body={body}")

    login_payload = {"username": username, "password": password}
    status, body = http_json("POST", login_url, login_payload)
    if not is_success(status, body):
        print(f"[WARN] login failed username={username} status={status} body={body}")
        return None
    data = body.get("data") or {}
    uid = data.get("uid")
    token = data.get("token")
    if uid is None or not token:
        print(f"[WARN] login response missing uid/token username={username} body={body}")
        return None
    return {
        "uid": int(uid),
        "username": username,
        "nickname": nickname,
        "password": password,
        "token": token
    }


def create_accounts(base_url: str, user_count: int, user_prefix: str, workers: int) -> List[Dict]:
    accounts: List[Dict] = []

    def job(index: int) -> Optional[Dict]:
        username = f"{user_prefix}{index:06d}"
        nickname = f"SimUser{index:06d}"
        password = f"SimPwd@{index:06d}"
        return register_or_login(base_url, username, nickname, password)

    max_workers = max(1, min(workers, user_count))
    completed = 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as pool:
        futures = [pool.submit(job, i) for i in range(1, user_count + 1)]
        for future in concurrent.futures.as_completed(futures):
            completed += 1
            account = future.result()
            if account:
                accounts.append(account)
            if completed % 50 == 0 or completed == user_count:
                print(f"[INFO] account progress {completed}/{user_count}, success={len(accounts)}")
    return accounts


def write_account_ledger(accounts: List[Dict], output_dir: Path) -> Tuple[Path, Path]:
    output_dir.mkdir(parents=True, exist_ok=True)
    csv_path = output_dir / "user_accounts.csv"
    json_path = output_dir / "user_accounts.json"

    with csv_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["uid", "username", "nickname", "password"])
        writer.writeheader()
        for item in accounts:
            writer.writerow({
                "uid": item["uid"],
                "username": item["username"],
                "nickname": item["nickname"],
                "password": item["password"]
            })

    with json_path.open("w", encoding="utf-8") as f:
        json.dump([
            {
                "uid": item["uid"],
                "username": item["username"],
                "nickname": item["nickname"],
                "password": item["password"]
            }
            for item in accounts
        ], f, ensure_ascii=False, indent=2)

    return csv_path, json_path


def create_one_video(base_url: str,
                     account: Dict,
                     upload_chunk_size: int,
                     video_min_bytes: int,
                     video_max_bytes: int,
                     media_base_url: str) -> Optional[Dict]:
    token = account.get("token")
    if not token:
        return None

    file_size = random.randint(video_min_bytes, video_max_bytes)
    video_bytes = os.urandom(file_size)
    md5_text = hashlib.md5(video_bytes).hexdigest()
    file_name = f"seed_{account['uid']}_{int(time.time() * 1000)}.mp4"
    total_chunks = (file_size + upload_chunk_size - 1) // upload_chunk_size

    init_payload = {
        "fileName": file_name,
        "totalSize": file_size,
        "chunkSize": upload_chunk_size,
        "totalChunks": total_chunks,
        "contentType": "video/mp4",
        "fileMd5": md5_text,
    }
    status, body = http_json(
        "POST",
        f"{base_url}/me/videos/uploads/init-session",
        init_payload,
        token=token,
        timeout=20,
    )
    if not is_success(status, body):
        return None
    upload_id = (body.get("data") or {}).get("uploadId")
    if not upload_id:
        return None

    for index in range(total_chunks):
        start = index * upload_chunk_size
        end = min(start + upload_chunk_size, file_size)
        chunk = video_bytes[start:end]
        status, body = http_multipart(
            "PUT",
            f"{base_url}/me/videos/uploads/{upload_id}/chunks/{index}",
            files=[("file", f"{file_name}.part{index}", "video/mp4", chunk)],
            token=token,
            timeout=30,
        )
        if not is_success(status, body):
            return None

    keyword = random.choice(VIDEO_KEYWORDS)
    complete_payload = {
        "title": f"{keyword} seed video {int(time.time() * 1000)}",
        "description": "seeded by api-only simulator",
        "coverUrl": f"{media_base_url}/cover/mock/{upload_id}.jpg",
        "duration": random.randint(30, 1800),
    }
    status, body = http_json(
        "POST",
        f"{base_url}/me/videos/uploads/{upload_id}/complete",
        complete_payload,
        token=token,
        timeout=25,
    )
    if not is_success(status, body):
        return None

    data = body.get("data") or {}
    video_id = data.get("videoId")
    if video_id is None:
        return None
    try:
        parsed_video_id = int(video_id)
    except (TypeError, ValueError):
        return None
    return {"videoId": parsed_video_id, "uid": account["uid"]}


def create_videos_via_api(base_url: str,
                          accounts: List[Dict],
                          video_count: int,
                          workers: int,
                          upload_chunk_size: int,
                          video_min_bytes: int,
                          video_max_bytes: int,
                          media_base_url: str) -> List[Dict]:
    if video_count <= 0:
        return []
    if not accounts:
        return []

    max_workers = max(1, min(workers, video_count))
    completed = 0
    created: List[Dict] = []

    def job(_: int) -> Optional[Dict]:
        account = random.choice(accounts)
        return create_one_video(
            base_url=base_url,
            account=account,
            upload_chunk_size=upload_chunk_size,
            video_min_bytes=video_min_bytes,
            video_max_bytes=video_max_bytes,
            media_base_url=media_base_url,
        )

    with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as pool:
        futures = [pool.submit(job, i) for i in range(video_count)]
        for future in concurrent.futures.as_completed(futures):
            completed += 1
            item = future.result()
            if item:
                created.append(item)
            if completed % 20 == 0 or completed == video_count:
                print(f"[INFO] video progress {completed}/{video_count}, success={len(created)}")
    return created


def write_video_ledger(videos: List[Dict], output_dir: Path) -> Tuple[Path, Path]:
    csv_path = output_dir / "seed_video_ids.csv"
    json_path = output_dir / "seed_video_ids.json"

    with csv_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["videoId", "uid"])
        writer.writeheader()
        for item in videos:
            writer.writerow({"videoId": item["videoId"], "uid": item["uid"]})

    with json_path.open("w", encoding="utf-8") as f:
        json.dump(videos, f, ensure_ascii=False, indent=2)

    return csv_path, json_path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Seed baseline users/videos by API only.")
    parser.add_argument("--base-url", default="http://localhost:8080", help="API base URL")
    parser.add_argument("--user-count", type=int, default=1000, help="How many users to register")
    parser.add_argument("--video-count", type=int, default=500, help="How many videos to create via API")
    parser.add_argument("--user-prefix", default="sim_u", help="Username prefix")
    parser.add_argument("--output-dir", default="tools/simulator/output", help="Output directory")
    parser.add_argument("--media-base-url", default="http://localhost:8080/media", help="Public media base URL")
    parser.add_argument("--seed", type=int, default=42, help="Random seed")
    parser.add_argument("--workers", type=int, default=20, help="Concurrent workers for register/login")
    parser.add_argument("--video-workers", type=int, default=16, help="Concurrent workers for video upload/complete")
    parser.add_argument("--upload-chunk-size", type=int, default=262144)
    parser.add_argument("--video-min-bytes", type=int, default=180000)
    parser.add_argument("--video-max-bytes", type=int, default=600000)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    random.seed(args.seed)

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    base_url = args.base_url.rstrip("/")

    print("[STEP] registering/logging users by API...")
    accounts = create_accounts(base_url, args.user_count, args.user_prefix, args.workers)
    if not accounts:
        print("[ERROR] no valid account created")
        return 1

    csv_path, json_path = write_account_ledger(accounts, output_dir)
    print(f"[OK] account ledger CSV:  {csv_path}")
    print(f"[OK] account ledger JSON: {json_path}")
    print(f"[OK] valid account count: {len(accounts)}")

    print("[STEP] creating baseline videos by upload APIs...")
    videos = create_videos_via_api(
        base_url=base_url,
        accounts=accounts,
        video_count=args.video_count,
        workers=args.video_workers,
        upload_chunk_size=args.upload_chunk_size,
        video_min_bytes=args.video_min_bytes,
        video_max_bytes=args.video_max_bytes,
        media_base_url=args.media_base_url.rstrip("/"),
    )
    if args.video_count > 0 and not videos:
        print("[ERROR] no video created by APIs")
        return 1

    video_csv, video_json = write_video_ledger(videos, output_dir)
    print(f"[OK] video ledger CSV:  {video_csv}")
    print(f"[OK] video ledger JSON: {video_json}")
    print(f"[OK] created video count: {len(videos)}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
