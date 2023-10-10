package com.danver.messengerserver.models;

import java.util.HashMap;
import java.util.Map;


public class MessageRequestDTO {

    private final Map<String, Object> requestData = new HashMap<>(); // Make it Concurrent?

    public void set(String key, Object value) {
        requestData.put(key, value);
    }

    public Object get(String key) {
        return requestData.get(key);
    }

    public void clear() {
        this.requestData.clear();
    }
}
