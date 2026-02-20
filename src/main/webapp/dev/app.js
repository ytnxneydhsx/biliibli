(function () {
    "use strict";

    const TOKEN_KEY = "dev_console_token";
    const responseBox = el("responseBox");

    let lastUploadFile = null;

    initBaseUrl();
    initToken();
    bindActions();

    function bindActions() {
        on("saveTokenBtn", () => {
            localStorage.setItem(TOKEN_KEY, tokenInput().value.trim());
            info("Token saved");
        });
        on("clearTokenBtn", () => {
            tokenInput().value = "";
            localStorage.removeItem(TOKEN_KEY);
            info("Token cleared");
        });

        on("registerBtn", async () => {
            await apiJson("POST", "/users/register", {
                username: val("regUsername"),
                nickname: val("regNickname"),
                password: val("regPassword"),
                confirmPassword: val("regConfirmPassword")
            }, false);
        });

        on("loginBtn", async () => {
            const res = await apiJson("POST", "/users/login", {
                username: val("loginUsername"),
                password: val("loginPassword")
            }, false);
            if (res && res.body && res.body.data && res.body.data.token) {
                tokenInput().value = res.body.data.token;
                localStorage.setItem(TOKEN_KEY, res.body.data.token);
                info("Login success, token saved");
            }
        });

        on("logoutBtn", async () => {
            await apiJson("POST", "/users/logout", null, true);
        });

        on("publicProfileBtn", async () => {
            const uid = longId("publicUid");
            await apiJson("GET", `/users/${uid}`, null, false);
        });

        on("updateProfileBtn", async () => {
            await apiJson("PUT", "/me/profile", {
                nickname: val("myNickname"),
                sign: val("mySign")
            }, true);
        });

        on("uploadAvatarBtn", async () => {
            const fileInput = el("avatarFile");
            if (!fileInput.files || fileInput.files.length === 0) {
                throw new Error("Select avatar file first.");
            }
            const form = new FormData();
            form.append("file", fileInput.files[0]);
            await apiForm("POST", "/me/avatar", form, true);
        });

        on("followersBtn", async () => {
            const uid = longId("followListUid");
            await apiJson("GET", `/users/${uid}/followers`, null, false);
        });
        on("followingsBtn", async () => {
            const uid = longId("followListUid");
            await apiJson("GET", `/users/${uid}/followings`, null, false);
        });
        on("friendsBtn", async () => {
            const uid = longId("followListUid");
            await apiJson("GET", `/users/${uid}/friends`, null, false);
        });
        on("followBtn", async () => {
            const targetUid = longId("targetUid");
            await apiJson("POST", `/me/followings/${targetUid}`, null, true);
        });
        on("unfollowBtn", async () => {
            const targetUid = longId("targetUid");
            await apiJson("DELETE", `/me/followings/${targetUid}`, null, true);
        });

        on("listVideosBtn", async () => {
            const pageNo = num("homePageNo");
            const pageSize = num("homePageSize");
            await apiJson("GET", `/videos?pageNo=${pageNo}&pageSize=${pageSize}`, null, false);
        });
        on("searchVideosBtn", async () => {
            const keyword = encodeURIComponent(val("searchKeyword"));
            const pageNo = num("searchPageNo");
            const pageSize = num("searchPageSize");
            await apiJson("GET", `/videos/search?keyword=${keyword}&pageNo=${pageNo}&pageSize=${pageSize}`, null, false);
        });
        on("userVideosBtn", async () => {
            const uid = longId("userVideoUid");
            const title = val("userVideoTitle");
            const pageNo = num("userVideoPageNo");
            const pageSize = num("userVideoPageSize");
            const titleQuery = title ? `&title=${encodeURIComponent(title)}` : "";
            await apiJson("GET", `/users/${uid}/videos?pageNo=${pageNo}&pageSize=${pageSize}${titleQuery}`, null, false);
        });
        on("videoDetailBtn", async () => {
            const videoId = longId("videoId");
            await apiJson("GET", `/videos/${videoId}`, null, false, true);
        });
        on("videoViewBtn", async () => {
            const videoId = longId("videoId");
            await apiJson("POST", `/videos/${videoId}/views`, null, true);
        });
        on("likeBtn", async () => {
            const videoId = longId("videoId");
            await apiJson("POST", `/me/videos/${videoId}/likes`, null, true);
        });
        on("unlikeBtn", async () => {
            const videoId = longId("videoId");
            await apiJson("DELETE", `/me/videos/${videoId}/likes`, null, true);
        });

        on("initUploadBtn", async () => {
            const fileInput = el("uploadFile");
            if (!fileInput.files || fileInput.files.length === 0) {
                throw new Error("Select video file first.");
            }
            const file = fileInput.files[0];
            lastUploadFile = file;
            const chunkSize = num("chunkSize");
            const totalChunks = Math.ceil(file.size / chunkSize);
            const contentType = val("uploadContentType") || file.type || null;
            const res = await apiJson("POST", "/me/videos/uploads/init-session", {
                fileName: file.name,
                totalSize: file.size,
                chunkSize: chunkSize,
                totalChunks: totalChunks,
                contentType: contentType,
                fileMd5: ""
            }, true);
            if (res && res.body && res.body.data && res.body.data.uploadId) {
                el("uploadId").value = res.body.data.uploadId;
                info("Upload session initialized.");
            }
        });

        on("uploadOneChunkBtn", async () => {
            ensureUploadFile();
            const uploadId = val("uploadId");
            const index = num("chunkIndex");
            await uploadChunk(uploadId, index, lastUploadFile, num("chunkSize"));
        });

        on("uploadAllChunksBtn", async () => {
            ensureUploadFile();
            const uploadId = val("uploadId");
            const chunkSize = num("chunkSize");
            const totalChunks = Math.ceil(lastUploadFile.size / chunkSize);
            for (let i = 0; i < totalChunks; i += 1) {
                await uploadChunk(uploadId, i, lastUploadFile, chunkSize);
            }
            info(`Uploaded all chunks: ${totalChunks}`);
        });

        on("uploadStatusBtn", async () => {
            const uploadId = val("uploadId");
            await apiJson("GET", `/me/videos/uploads/${uploadId}`, null, true);
        });

        on("uploadCompleteBtn", async () => {
            const uploadId = val("uploadId");
            await apiJson("POST", `/me/videos/uploads/${uploadId}/complete`, {
                title: val("completeTitle"),
                description: val("completeDesc"),
                coverUrl: val("completeCoverUrl"),
                duration: num("completeDuration")
            }, true);
        });
    }

    async function uploadChunk(uploadId, index, file, chunkSize) {
        if (!uploadId) {
            throw new Error("uploadId is required.");
        }
        const start = index * chunkSize;
        const end = Math.min(start + chunkSize, file.size);
        const chunkBlob = file.slice(start, end);

        const form = new FormData();
        form.append("file", chunkBlob, `${file.name}.part${index}`);
        await apiForm("PUT", `/me/videos/uploads/${uploadId}/chunks/${index}`, form, true);
    }

    async function apiJson(method, path, body, authRequired, attachAuthIfPresent) {
        const headers = {
            "Accept": "application/json"
        };
        if (body !== null && body !== undefined) {
            headers["Content-Type"] = "application/json";
        }
        const withAuth = Boolean(authRequired) || (attachAuthIfPresent && tokenInput().value.trim());
        attachToken(headers, withAuth);

        const url = fullUrl(path);
        const started = performance.now();
        const response = await fetch(url, {
            method: method,
            headers: headers,
            body: body !== null && body !== undefined ? JSON.stringify(body) : undefined
        });
        return renderResponse(url, method, body, response, started);
    }

    async function apiForm(method, path, formData, authRequired) {
        const headers = {};
        attachToken(headers, authRequired);

        const url = fullUrl(path);
        const started = performance.now();
        const response = await fetch(url, {
            method: method,
            headers: headers,
            body: formData
        });
        return renderResponse(url, method, "[multipart/form-data]", response, started);
    }

    async function renderResponse(url, method, requestBody, response, started) {
        const costMs = Math.round(performance.now() - started);
        const text = await response.text();
        let body;
        try {
            body = text ? JSON.parse(text) : null;
        } catch (e) {
            body = text;
        }
        const output = {
            request: {
                method: method,
                url: url,
                body: requestBody
            },
            response: {
                status: response.status,
                ok: response.ok,
                costMs: costMs,
                body: body
            }
        };
        responseBox.textContent = JSON.stringify(output, null, 2);
        return { response: response, body: body };
    }

    function attachToken(headers, required) {
        const token = tokenInput().value.trim();
        if (!token && required) {
            throw new Error("JWT token is required for this API.");
        }
        if (token) {
            headers["Authorization"] = `Bearer ${token}`;
        }
    }

    function initBaseUrl() {
        const contextPath = detectContextPath();
        el("apiBase").value = `${window.location.origin}${contextPath}`;
    }

    function initToken() {
        const saved = localStorage.getItem(TOKEN_KEY);
        if (saved) {
            tokenInput().value = saved;
        }
    }

    function detectContextPath() {
        const marker = "/dev/";
        const p = window.location.pathname;
        const idx = p.indexOf(marker);
        if (idx < 0) {
            return "";
        }
        return p.substring(0, idx);
    }

    function ensureUploadFile() {
        const fileInput = el("uploadFile");
        if (!fileInput.files || fileInput.files.length === 0) {
            throw new Error("Select video file first.");
        }
        lastUploadFile = fileInput.files[0];
    }

    function fullUrl(path) {
        const base = el("apiBase").value.trim().replace(/\/+$/, "");
        const normalized = path.startsWith("/") ? path : `/${path}`;
        return `${base}${normalized}`;
    }

    function tokenInput() {
        return el("token");
    }

    function val(id) {
        return el(id).value.trim();
    }

    function num(id) {
        const value = el(id).value;
        if (value === null || value === undefined || value === "") {
            return 0;
        }
        return Number(value);
    }

    function longId(id) {
        const value = val(id);
        if (!/^\d+$/.test(value)) {
            throw new Error(`${id} must be a positive integer string.`);
        }
        return value;
    }

    function el(id) {
        const node = document.getElementById(id);
        if (!node) {
            throw new Error(`Element not found: ${id}`);
        }
        return node;
    }

    function on(id, handler) {
        el(id).addEventListener("click", async () => {
            try {
                await handler();
            } catch (e) {
                responseBox.textContent = JSON.stringify({
                    error: String(e && e.message ? e.message : e)
                }, null, 2);
            }
        });
    }

    function info(message) {
        responseBox.textContent = JSON.stringify({ info: message }, null, 2);
    }
})();
