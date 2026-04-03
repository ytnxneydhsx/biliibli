package com.bilibili.im.websocket.connection.impl;

import com.bilibili.im.websocket.connection.ImSessionConnection;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;

public class NettySessionConnection implements ImSessionConnection {

    private static final WebSocketCloseStatus HEARTBEAT_TIMEOUT_STATUS =
            new WebSocketCloseStatus(4500, "heartbeat_timeout");

    private final Long userId;
    private final Channel channel;

    public NettySessionConnection(Long userId, Channel channel) {
        this.userId = userId;
        this.channel = channel;
    }

    @Override
    public String getId() {
        return channel == null ? null : channel.id().asLongText();
    }

    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public boolean isOpen() {
        return channel != null && channel.isOpen() && channel.isActive();
    }

    @Override
    public void sendText(String text) throws Exception {
        if (channel == null) {
            throw new IllegalStateException("channel is null");
        }
        ChannelFuture future = channel.writeAndFlush(new TextWebSocketFrame(text));
        future.syncUninterruptibly();
        if (!future.isSuccess()) {
            Throwable cause = future.cause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new IllegalStateException("failed to send websocket frame", cause);
        }
    }

    @Override
    public void close(String reason) throws Exception {
        if (!isOpen()) {
            return;
        }

        CloseWebSocketFrame closeFrame = "heartbeat_timeout".equals(reason)
                ? new CloseWebSocketFrame(HEARTBEAT_TIMEOUT_STATUS)
                : new CloseWebSocketFrame(WebSocketCloseStatus.NORMAL_CLOSURE);

        ChannelFuture future = channel.writeAndFlush(closeFrame);
        future.addListener(ChannelFutureListener.CLOSE);
        future.syncUninterruptibly();
        if (!future.isSuccess()) {
            Throwable cause = future.cause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new IllegalStateException("failed to close websocket channel", cause);
        }
    }
}
