(function () {
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
        messageStream: document.getElementById("messageStream"),
        messageInput: document.getElementById("messageInput"),
        chatTitle: document.getElementById("chatTitle"),
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

        state.apiBase = localStorage.getItem("imLab.apiBase") || defaultApiBase;
        state.wsBase = localStorage.getItem("imLab.wsBase") || defaultWsBase;
        state.token = localStorage.getItem("imLab.token") || "";
        state.currentUid = localStorage.getItem("imLab.currentUid") || "";
        state.activePeerUid = localStorage.getItem("imLab.activePeerUid") || "";

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

        if (state.activePeerUid) {
            updateChatTitle(state.activePeerUid);
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
                body: JSON.stringify({
                    username,
                    password
                })
            });
            const result = await response.json();
            if (!response.ok || !result || result.code !== 0 || !result.data) {
                throw new Error(result && result.message ? result.message : "登录失败");
            }

            state.token = result.data.token || "";
            state.currentUid = String(result.data.uid || "");
            elements.token.value = state.token;
            elements.currentUid.value = state.currentUid;
            localStorage.setItem("imLab.token", state.token);
            localStorage.setItem("imLab.currentUid", state.currentUid);
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
            pushEvent("ui", "WebSocket 已连接。");
            return;
        }

        const wsUrl = buildSocketUrl(elements.wsUrl.value.trim(), state.token);
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
        const pending = state.pendingMessages.get(String(data.sendTime || "")) || findPendingByConversationId(data.conversationId);
        if (!pending) {
            return;
        }

        pending.pending = false;
        pending.time = formatDateTime(data.sendTime);
        appendMessage(pending.peerUid, pending);
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
            text,
            pending: false,
            clientKey: "received-" + (data.clientMessageId || Date.now())
        });

        if (!state.activePeerUid) {
            setActivePeer(peerUid);
        } else if (state.activePeerUid === peerUid) {
            renderMessages();
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

        state.conversations.set(peerUid, {
            peerUid,
            conversationId: data.conversationId,
            lastMessage: data.lastMessage || "",
            lastMessageTime: formatDateTime(data.lastMessageTime),
            unreadCount: Number(data.unreadCount || 0)
        });

        if (!state.activePeerUid) {
            setActivePeer(peerUid);
        }
        renderConversationList();
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
            clientMessageId,
            messageType: 1,
            content: {
                text,
                imageUrls: []
            }
        };

        const optimisticMessage = {
            direction: "outgoing",
            senderId: state.currentUid,
            time: "发送中",
            text,
            pending: true,
            peerUid,
            clientKey
        };

        state.pendingMessages.set(clientKey, optimisticMessage);
        appendMessage(peerUid, optimisticMessage);
        setActivePeer(peerUid);
        renderMessages();

        const existingConversation = state.conversations.get(peerUid) || {};
        state.conversations.set(peerUid, {
            peerUid,
            conversationId: existingConversation.conversationId || "",
            lastMessage: text,
            lastMessageTime: "刚刚",
            unreadCount: existingConversation.unreadCount || 0
        });
        renderConversationList();

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

    function renderMessages() {
        const peerUid = state.activePeerUid;
        elements.messageStream.innerHTML = "";

        if (!peerUid) {
            elements.messageStream.innerHTML = "<div class=\"empty-state\">先登录并选择一个会话，我们就可以开始测试实时消息链路了。</div>";
            return;
        }

        const messages = state.messagesByPeer.get(peerUid) || [];
        if (!messages.length) {
            elements.messageStream.innerHTML = "<div class=\"empty-state\">当前会话还没有实时消息。这个页面不会主动拉历史消息，只展示当前在线期间的推送结果。</div>";
            return;
        }

        messages.forEach(function (item) {
            const bubble = document.createElement("article");
            bubble.className = "bubble " + item.direction + (item.pending ? " pending" : "");
            bubble.innerHTML =
                "<div class=\"bubble-meta\">" +
                "<span>" + escapeHtml(item.direction === "outgoing" ? "我" : "UID " + escapeHtml(item.senderId || "")) + "</span>" +
                "<span>" + escapeHtml(item.time || "") + "</span>" +
                "</div>" +
                "<p class=\"bubble-text\">" + escapeHtml(item.text || "") + "</p>";
            elements.messageStream.appendChild(bubble);
        });
        elements.messageStream.scrollTop = elements.messageStream.scrollHeight;
    }

    function renderConversationList() {
        elements.conversationList.innerHTML = "";
        const items = Array.from(state.conversations.values())
            .sort(function (left, right) {
                return String(right.lastMessageTime || "").localeCompare(String(left.lastMessageTime || ""));
            });

        if (!items.length) {
            elements.conversationList.innerHTML = "<div class=\"empty-state conversation-empty\">当前还没有收到窗口推送。你也可以先手动输入对方 uid 进入测试。</div>";
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
        localStorage.setItem("imLab.activePeerUid", state.activePeerUid);
        elements.peerUid.value = state.activePeerUid;
        updateChatTitle(state.activePeerUid);
        renderConversationList();
        renderMessages();
    }

    function updateChatTitle(peerUid) {
        elements.chatTitle.textContent = peerUid ? "和 UID " + peerUid + " 的实时会话" : "尚未选择会话";
    }

    function applyManualPeer() {
        const peerUid = elements.peerUid.value.trim();
        if (!peerUid) {
            pushEvent("ui", "请输入对方 uid。");
            return;
        }
        if (!state.conversations.has(peerUid)) {
            state.conversations.set(peerUid, {
                peerUid,
                conversationId: "",
                lastMessage: "手动创建的测试会话",
                lastMessageTime: "待发送",
                unreadCount: 0
            });
        }
        setActivePeer(peerUid);
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
            return;
        }
        elements.loginStatus.className = "status-pill is-muted";
        elements.loginStatus.textContent = "未登录";
    }

    function syncApiBase() {
        state.apiBase = trimTrailingSlash(elements.apiBase.value.trim() || window.location.origin);
        elements.apiBase.value = state.apiBase;
        localStorage.setItem("imLab.apiBase", state.apiBase);
    }

    function syncWsBase() {
        state.wsBase = elements.wsUrl.value.trim() || buildDefaultWsBase(state.apiBase || window.location.origin);
        elements.wsUrl.value = state.wsBase;
        localStorage.setItem("imLab.wsBase", state.wsBase);
    }

    function syncToken() {
        state.token = elements.token.value.trim();
        localStorage.setItem("imLab.token", state.token);
        updateLoginStatus();
    }

    function syncCurrentUid() {
        state.currentUid = elements.currentUid.value.trim();
        localStorage.setItem("imLab.currentUid", state.currentUid);
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

    function extractText(content) {
        if (!content) {
            return "";
        }
        const text = (content.text || "").trim();
        if (text) {
            return text;
        }
        return "[非文本消息]";
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
        if (left <= right) {
            return "single_" + left + "_" + right;
        }
        return "single_" + right + "_" + left;
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
