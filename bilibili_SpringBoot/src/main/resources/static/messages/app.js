(function () {
    const storagePrefix = "biliMessages.";
    const state = {
        apiBase: "",
        wsBase: "",
        token: "",
        currentUid: "",
        activePeerUid: "",
        socket: null,
        heartbeatTimer: null,
        conversations: new Map(),
        messagesByPeer: new Map(),
        pendingMessages: new Map()
    };

    const elements = {
        apiBase: document.getElementById("apiBase"),
        username: document.getElementById("username"),
        password: document.getElementById("password"),
        token: document.getElementById("token"),
        currentUid: document.getElementById("currentUid"),
        wsUrl: document.getElementById("wsUrl"),
        peerUid: document.getElementById("peerUid"),
        connectionStatus: document.getElementById("connectionStatus"),
        loginStatus: document.getElementById("loginStatus"),
        conversationList: document.getElementById("conversationList"),
        conversationCount: document.getElementById("conversationCount"),
        messageStream: document.getElementById("messageStream"),
        messageInput: document.getElementById("messageInput"),
        chatTitle: document.getElementById("chatTitle"),
        chatSubtitle: document.getElementById("chatSubtitle"),
        accountPrimary: document.getElementById("accountPrimary"),
        eventLog: document.getElementById("eventLog"),
        loginForm: document.getElementById("loginForm"),
        sendForm: document.getElementById("sendForm"),
        connectButton: document.getElementById("connectButton"),
        disconnectButton: document.getElementById("disconnectButton"),
        usePeerButton: document.getElementById("usePeerButton")
    };

    initialize();

    function initialize() {
        const defaultApiBase = window.location.origin;
        const defaultWsBase = buildDefaultWsBase(defaultApiBase);
        const peerUidFromQuery = parsePeerUidFromQuery();

        state.apiBase = localStorage.getItem(storagePrefix + "apiBase") || defaultApiBase;
        state.wsBase = localStorage.getItem(storagePrefix + "wsBase") || defaultWsBase;
        state.token = localStorage.getItem(storagePrefix + "token") || "";
        state.currentUid = localStorage.getItem(storagePrefix + "currentUid") || "";
        state.activePeerUid = peerUidFromQuery || localStorage.getItem(storagePrefix + "activePeerUid") || "";

        elements.apiBase.value = state.apiBase;
        elements.wsUrl.value = state.wsBase;
        elements.token.value = state.token;
        elements.currentUid.value = state.currentUid;
        elements.peerUid.value = state.activePeerUid;

        bindEvents();
        updateLoginStatus();
        updateConnectionStatus("idle", "未连接");
        renderConversationList();
        renderMessages();
        updateChatTitle(state.activePeerUid);

        if (state.activePeerUid) {
            ensureConversationExists(state.activePeerUid);
            renderConversationList();
        }
    }

    function bindEvents() {
        elements.loginForm.addEventListener("submit", handleLogin);
        elements.connectButton.addEventListener("click", connectSocket);
        elements.disconnectButton.addEventListener("click", disconnectSocket);
        elements.usePeerButton.addEventListener("click", applyManualPeer);
        elements.sendForm.addEventListener("submit", handleSendMessage);
        elements.apiBase.addEventListener("change", syncApiBase);
        elements.wsUrl.addEventListener("change", syncWsBase);
        elements.token.addEventListener("change", syncToken);
        elements.currentUid.addEventListener("change", syncCurrentUid);
    }

    async function handleLogin(event) {
        event.preventDefault();
        syncApiBase();

        const username = elements.username.value.trim();
        const password = elements.password.value;
        if (!username || !password) {
            pushEvent("ui", "请输入用户名和密码。");
            return;
        }

        try {
            const response = await fetch(joinUrl(state.apiBase, "/users/login"), {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ username, password })
            });
            const result = await response.json();
            if (!response.ok || !result || result.code !== 0 || !result.data) {
                throw new Error(result && result.message ? result.message : "登录失败");
            }

            state.token = result.data.token || "";
            state.currentUid = String(result.data.uid || "");
            elements.token.value = state.token;
            elements.currentUid.value = state.currentUid;
            localStorage.setItem(storagePrefix + "token", state.token);
            localStorage.setItem(storagePrefix + "currentUid", state.currentUid);
            updateLoginStatus();
            pushEvent("login", "登录成功，已获取 token。");
        } catch (error) {
            pushEvent("error", "登录失败：" + normalizeError(error));
        }
    }

    function connectSocket() {
        syncWsBase();
        syncToken();
        syncCurrentUid();

        if (!state.token) {
            pushEvent("ui", "请先登录或手动填写 token。");
            return;
        }
        if (state.socket && state.socket.readyState === WebSocket.OPEN) {
            pushEvent("ui", "实时通道已经连上了。");
            return;
        }

        const wsUrl = buildSocketUrl(state.wsBase, state.token);
        try {
            const socket = new WebSocket(wsUrl);
            state.socket = socket;
            updateConnectionStatus("idle", "连接中");
            pushEvent("ws", "开始连接 " + wsUrl);

            socket.addEventListener("open", function () {
                updateConnectionStatus("live", "已连接");
                startHeartbeat();
                pushEvent("ws", "WebSocket 已连接。");
            });

            socket.addEventListener("message", function (messageEvent) {
                handleSocketMessage(messageEvent.data);
            });

            socket.addEventListener("close", function () {
                stopHeartbeat();
                updateConnectionStatus("idle", "已断开");
                pushEvent("ws", "WebSocket 已关闭。");
            });

            socket.addEventListener("error", function () {
                pushEvent("error", "WebSocket 发生错误。");
            });
        } catch (error) {
            pushEvent("error", "连接失败：" + normalizeError(error));
        }
    }

    function disconnectSocket() {
        stopHeartbeat();
        if (state.socket) {
            state.socket.close();
            state.socket = null;
        }
        updateConnectionStatus("idle", "未连接");
    }

    function handleSocketMessage(raw) {
        let packet;
        try {
            packet = JSON.parse(raw);
        } catch (error) {
            pushEvent("raw", raw);
            return;
        }

        const type = packet.type || "unknown";
        pushEvent(type, JSON.stringify(packet.data || packet.message || packet, null, 2));

        if (type === "heartbeat_ack") {
            return;
        }
        if (type === "send_message_accepted") {
            handleMessageAccepted(packet.data);
            return;
        }
        if (type === "message_received") {
            handleMessageReceived(packet.data);
            return;
        }
        if (type === "conversation_updated") {
            handleConversationUpdated(packet.data);
            return;
        }
        if (type === "error") {
            updateConnectionStatus("error", "连接异常");
        }
    }

    function handleMessageAccepted(data) {
        if (!data) {
            return;
        }
        const pending = state.pendingMessages.get(String(data.clientMessageId || "")) || findPendingByConversationId(data.conversationId);
        if (!pending) {
            return;
        }

        pending.pending = false;
        pending.time = formatDateTime(data.sendTime);
        pending.epoch = toEpoch(data.sendTime);
        state.pendingMessages.delete(pending.clientKey);
        renderMessages();
    }

    function handleMessageReceived(data) {
        if (!data) {
            return;
        }
        const senderId = String(data.senderId || "");
        const receiverId = String(data.receiverId || "");
        const currentUid = String(state.currentUid || "");
        const peerUid = senderId === currentUid ? receiverId : senderId;
        const text = extractText(data.content);

        appendMessage(peerUid, {
            direction: senderId === currentUid ? "outgoing" : "incoming",
            senderId,
            time: formatDateTime(data.sendTime),
            epoch: toEpoch(data.sendTime),
            text,
            pending: false,
            clientKey: "received-" + (data.clientMessageId || Date.now())
        });

        upsertConversation(peerUid, {
            conversationId: data.conversationId || buildSingleConversationId(currentUid, peerUid),
            lastMessage: text,
            lastMessageTime: formatDateTime(data.sendTime),
            lastMessageEpoch: toEpoch(data.sendTime),
            unreadCount: state.activePeerUid === peerUid || senderId === currentUid ? 0 : 1
        });

        if (!state.activePeerUid) {
            setActivePeer(peerUid);
        } else {
            renderConversationList();
            if (state.activePeerUid === peerUid) {
                renderMessages();
            }
        }
    }

    function handleConversationUpdated(data) {
        if (!data || !data.conversationId) {
            return;
        }
        const peerUid = resolvePeerUidFromConversation(data.conversationId, state.currentUid);
        if (!peerUid) {
            return;
        }

        upsertConversation(peerUid, {
            conversationId: data.conversationId,
            lastMessage: data.lastMessage || "",
            lastMessageTime: formatDateTime(data.lastMessageTime),
            lastMessageEpoch: toEpoch(data.lastMessageTime),
            unreadCount: Number(data.unreadCount || 0)
        });

        if (!state.activePeerUid) {
            setActivePeer(peerUid);
        } else {
            renderConversationList();
        }
    }

    function handleSendMessage(event) {
        event.preventDefault();
        const peerUid = elements.peerUid.value.trim();
        const text = elements.messageInput.value.trim();

        if (!peerUid) {
            pushEvent("ui", "请先选择对方 uid。");
            return;
        }
        if (!text) {
            pushEvent("ui", "请输入消息内容。");
            return;
        }
        if (!state.socket || state.socket.readyState !== WebSocket.OPEN) {
            pushEvent("ui", "WebSocket 还未连接。");
            return;
        }

        const clientMessageId = Date.now();
        const clientKey = String(clientMessageId);
        const payload = {
            type: "send_message",
            receiverId: Number(peerUid),
            clientMessageId: clientMessageId,
            messageType: 1,
            content: {
                text: text,
                imageUrls: []
            }
        };

        const optimisticMessage = {
            direction: "outgoing",
            senderId: state.currentUid,
            time: "发送中",
            epoch: Date.now(),
            text: text,
            pending: true,
            peerUid: peerUid,
            clientKey: clientKey
        };

        state.pendingMessages.set(clientKey, optimisticMessage);
        appendMessage(peerUid, optimisticMessage);
        upsertConversation(peerUid, {
            conversationId: buildSingleConversationId(state.currentUid, peerUid),
            lastMessage: text,
            lastMessageTime: "刚刚",
            lastMessageEpoch: Date.now(),
            unreadCount: 0
        });

        setActivePeer(peerUid);
        renderMessages();
        state.socket.send(JSON.stringify(payload));
        elements.messageInput.value = "";
    }

    function appendMessage(peerUid, message) {
        if (!peerUid) {
            return;
        }
        const list = state.messagesByPeer.get(peerUid) || [];
        list.push(message);
        state.messagesByPeer.set(peerUid, list);
    }

    function upsertConversation(peerUid, payload) {
        const current = state.conversations.get(peerUid) || {
            peerUid: peerUid,
            conversationId: "",
            lastMessage: "",
            lastMessageTime: "",
            lastMessageEpoch: 0,
            unreadCount: 0
        };
        state.conversations.set(peerUid, {
            peerUid: peerUid,
            conversationId: payload.conversationId || current.conversationId,
            lastMessage: payload.lastMessage || current.lastMessage,
            lastMessageTime: payload.lastMessageTime || current.lastMessageTime,
            lastMessageEpoch: payload.lastMessageEpoch || current.lastMessageEpoch || 0,
            unreadCount: Number(payload.unreadCount != null ? payload.unreadCount : current.unreadCount || 0)
        });
    }

    function renderMessages() {
        const peerUid = state.activePeerUid;
        elements.messageStream.innerHTML = "";

        if (!peerUid) {
            elements.messageStream.innerHTML = "<div class=\"empty-state\">先选中一个会话，我们就可以开始测试实时私信链路了。</div>";
            return;
        }

        const messages = state.messagesByPeer.get(peerUid) || [];
        if (!messages.length) {
            elements.messageStream.innerHTML = "<div class=\"empty-state\">这个会话当前还没有实时消息。页面不会主动拉历史记录，只展示你在线期间收到的推送。</div>";
            return;
        }

        messages.forEach(function (item) {
            const bubble = document.createElement("article");
            bubble.className = "bubble " + item.direction + (item.pending ? " pending" : "");
            bubble.innerHTML =
                "<div class=\"bubble-meta\">" +
                "<span>" + escapeHtml(item.direction === "outgoing" ? "我" : "UID " + String(item.senderId || "")) + "</span>" +
                "<span>" + escapeHtml(item.time || "") + "</span>" +
                "</div>" +
                "<p class=\"bubble-text\">" + escapeHtml(item.text || "") + "</p>";
            elements.messageStream.appendChild(bubble);
        });

        elements.messageStream.scrollTop = elements.messageStream.scrollHeight;
    }

    function renderConversationList() {
        elements.conversationList.innerHTML = "";
        const items = Array.from(state.conversations.values()).sort(function (left, right) {
            return Number(right.lastMessageEpoch || 0) - Number(left.lastMessageEpoch || 0);
        });

        elements.conversationCount.textContent = items.length ? ("共 " + items.length + " 个会话") : "暂无会话";

        if (!items.length) {
            elements.conversationList.innerHTML = "<div class=\"conversation-empty\">当前还没有收到窗口更新推送。你也可以先手动输入对方 uid 进入测试。</div>";
            return;
        }

        items.forEach(function (conversation) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "conversation-card" + (state.activePeerUid === conversation.peerUid ? " is-active" : "");
            button.innerHTML =
                "<div class=\"conversation-top\">" +
                "<span class=\"conversation-title\">UID " + escapeHtml(conversation.peerUid) + "</span>" +
                "<span class=\"conversation-time\">" + escapeHtml(conversation.lastMessageTime || "") + "</span>" +
                "</div>" +
                "<div class=\"conversation-summary\">" + escapeHtml(conversation.lastMessage || "暂无摘要") + "</div>" +
                (conversation.unreadCount > 0 ? "<div class=\"conversation-top\"><span class=\"conversation-unread\">" + conversation.unreadCount + "</span></div>" : "");
            button.addEventListener("click", function () {
                setActivePeer(conversation.peerUid);
            });
            elements.conversationList.appendChild(button);
        });
    }

    function setActivePeer(peerUid) {
        if (!peerUid) {
            return;
        }
        state.activePeerUid = String(peerUid);
        localStorage.setItem(storagePrefix + "activePeerUid", state.activePeerUid);
        elements.peerUid.value = state.activePeerUid;

        const conversation = state.conversations.get(state.activePeerUid);
        if (conversation) {
            conversation.unreadCount = 0;
        }

        updateChatTitle(state.activePeerUid);
        renderConversationList();
        renderMessages();
    }

    function updateChatTitle(peerUid) {
        if (!peerUid) {
            elements.chatTitle.textContent = "请选择一个会话";
            elements.chatSubtitle.textContent = "这页更像站内私信页，但不会伪装出历史分页、图片上传、已读回执这些你后端还没支持的功能。";
            return;
        }
        elements.chatTitle.textContent = "与 UID " + peerUid + " 的对话";
        elements.chatSubtitle.textContent = "当前只展示在线期间的实时消息与窗口更新。";
    }

    function applyManualPeer() {
        const peerUid = elements.peerUid.value.trim();
        if (!peerUid) {
            pushEvent("ui", "请输入对方 uid。");
            return;
        }
        ensureConversationExists(peerUid);
        setActivePeer(peerUid);
    }

    function ensureConversationExists(peerUid) {
        if (!peerUid || state.conversations.has(peerUid)) {
            return;
        }
        upsertConversation(peerUid, {
            conversationId: state.currentUid ? buildSingleConversationId(state.currentUid, peerUid) : "",
            lastMessage: "从用户主页进入的会话",
            lastMessageTime: "待发送",
            lastMessageEpoch: Date.now(),
            unreadCount: 0
        });
    }

    function startHeartbeat() {
        stopHeartbeat();
        state.heartbeatTimer = window.setInterval(function () {
            if (!state.socket || state.socket.readyState !== WebSocket.OPEN) {
                return;
            }
            state.socket.send(JSON.stringify({ type: "heartbeat" }));
        }, 30000);
    }

    function stopHeartbeat() {
        if (state.heartbeatTimer) {
            window.clearInterval(state.heartbeatTimer);
            state.heartbeatTimer = null;
        }
    }

    function pushEvent(type, body) {
        const item = document.createElement("article");
        item.className = "event-item";
        item.innerHTML =
            "<div class=\"event-top\">" +
            "<span class=\"event-type\">" + escapeHtml(type) + "</span>" +
            "<span class=\"event-meta\">" + escapeHtml(formatTime(new Date())) + "</span>" +
            "</div>" +
            "<pre class=\"event-body\">" + escapeHtml(String(body || "")) + "</pre>";
        elements.eventLog.prepend(item);
    }

    function updateConnectionStatus(kind, text) {
        elements.connectionStatus.className = "status-pill " + statusClassName(kind);
        elements.connectionStatus.textContent = text;
    }

    function updateLoginStatus() {
        if (state.currentUid && state.token) {
            elements.loginStatus.className = "status-pill is-ok";
            elements.loginStatus.textContent = "UID " + state.currentUid;
            elements.accountPrimary.textContent = "UID " + state.currentUid;
            return;
        }
        elements.loginStatus.className = "status-pill is-muted";
        elements.loginStatus.textContent = "未登录";
        elements.accountPrimary.textContent = "未登录";
    }

    function syncApiBase() {
        state.apiBase = trimTrailingSlash(elements.apiBase.value.trim() || window.location.origin);
        elements.apiBase.value = state.apiBase;
        localStorage.setItem(storagePrefix + "apiBase", state.apiBase);
    }

    function syncWsBase() {
        state.wsBase = elements.wsUrl.value.trim() || buildDefaultWsBase(state.apiBase || window.location.origin);
        elements.wsUrl.value = state.wsBase;
        localStorage.setItem(storagePrefix + "wsBase", state.wsBase);
    }

    function syncToken() {
        state.token = elements.token.value.trim();
        localStorage.setItem(storagePrefix + "token", state.token);
        updateLoginStatus();
    }

    function syncCurrentUid() {
        state.currentUid = elements.currentUid.value.trim();
        localStorage.setItem(storagePrefix + "currentUid", state.currentUid);
        updateLoginStatus();
    }

    function buildDefaultWsBase(apiBase) {
        const url = new URL(apiBase, window.location.origin);
        const protocol = url.protocol === "https:" ? "wss:" : "ws:";
        return protocol + "//" + url.host + "/ws/im";
    }

    function buildSocketUrl(base, token) {
        const url = new URL(base);
        url.searchParams.set("token", token);
        return url.toString();
    }

    function joinUrl(base, path) {
        return trimTrailingSlash(base) + path;
    }

    function trimTrailingSlash(value) {
        return value.replace(/\/+$/, "");
    }

    function parsePeerUidFromQuery() {
        const params = new URLSearchParams(window.location.search);
        const peerUid = (params.get("peerUid") || "").trim();
        return /^\d+$/.test(peerUid) ? peerUid : "";
    }

    function extractText(content) {
        if (!content) {
            return "";
        }
        const text = String(content.text || "").trim();
        return text || "[非文本消息]";
    }

    function resolvePeerUidFromConversation(conversationId, currentUid) {
        const match = /^single_(\d+)_(\d+)$/.exec(String(conversationId || ""));
        if (!match) {
            return "";
        }
        const first = match[1];
        const second = match[2];
        return first === String(currentUid) ? second : first;
    }

    function findPendingByConversationId(conversationId) {
        const items = Array.from(state.pendingMessages.values());
        return items.find(function (item) {
            const peerUid = String(item.peerUid || "");
            if (!peerUid || !state.currentUid) {
                return false;
            }
            return buildSingleConversationId(state.currentUid, peerUid) === conversationId;
        });
    }

    function buildSingleConversationId(first, second) {
        const left = Number(first);
        const right = Number(second);
        return left <= right ? ("single_" + left + "_" + right) : ("single_" + right + "_" + left);
    }

    function formatDateTime(value) {
        if (!value) {
            return "";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return String(value);
        }
        return formatTime(date);
    }

    function formatTime(date) {
        return new Intl.DateTimeFormat("zh-CN", {
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit"
        }).format(date);
    }

    function toEpoch(value) {
        if (!value) {
            return 0;
        }
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? 0 : date.getTime();
    }

    function statusClassName(kind) {
        if (kind === "live") {
            return "is-live";
        }
        if (kind === "ok") {
            return "is-ok";
        }
        if (kind === "error") {
            return "is-error";
        }
        return "is-idle";
    }

    function normalizeError(error) {
        if (!error) {
            return "unknown error";
        }
        return error.message || String(error);
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }
})();
