package com.danver.messengerserver.utils;

public class Constants {
    public static final String USER_JWT_EMAIL_KEY = "email";
    public static final String CHATS_ONE_PAGE_COUNT = "50";
    public static final String WS_MESSAGE_SERVICE_STOMP_ENDPOINT = "/ws";

    // Path prefix for incoming requests to be processed by methods with @MessageMapping annotation
    public static final String WS_MESSAGE_SERVICE_APP_PREFIX = "/websocket";

    // Path prefix where we send back messages
    public static final String WS_MESSAGE_SERVICE_TOPIC = "/topic";
    public static final String WS_MESSAGE_SERVICE_USER_DESTINATION_PREFIX ="/user";

    public static final String WS_MESSAGE_SERVICE_PRIVATE_CHAT_QUEUE_NAME = "/topic/chat/private/queue/messages";

    public static final String WS_MESSAGE_SERVICE_CHAT_QUEUE_NAME = "/topic/chat/queue/messages";

}
