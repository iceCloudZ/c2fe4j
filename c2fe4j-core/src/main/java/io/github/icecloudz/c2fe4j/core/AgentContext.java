package io.github.icecloudz.c2fe4j.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable context carried through an agent loop iteration.
 * Holds conversation history, retrieved data, and arbitrary state.
 */
public class AgentContext {

    private final List<ChatMessage> messages = new ArrayList<>();
    private final Map<String, Object> state = new HashMap<>();
    private String sessionId;
    private int stepCount;
    private int totalTokens;

    public AgentContext(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<ChatMessage> messages() {
        return messages;
    }

    public AgentContext addMessage(ChatMessage message) {
        messages.add(message);
        return this;
    }

    public Map<String, Object> state() {
        return state;
    }

    public AgentContext set(String key, Object value) {
        state.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) state.get(key);
    }

    public String sessionId() {
        return sessionId;
    }

    public int stepCount() {
        return stepCount;
    }

    public AgentContext incrementStep() {
        stepCount++;
        return this;
    }

    public int totalTokens() {
        return totalTokens;
    }

    public AgentContext addTokens(int tokens) {
        totalTokens += tokens;
        return this;
    }
}
