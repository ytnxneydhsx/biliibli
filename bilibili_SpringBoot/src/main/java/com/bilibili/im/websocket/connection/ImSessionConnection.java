package com.bilibili.im.websocket.connection;

public interface ImSessionConnection {

    String getId();

    Long getUserId();

    boolean isOpen();

    void sendText(String text) throws Exception;

    void close(String reason) throws Exception;
}
