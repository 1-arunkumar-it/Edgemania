package io.edgemania.model;

import java.util.UUID;

public record Edge(
        String id,
        String from,
        String fromSocket,
        String to,
        String toSocket
) {
    public Edge(String from, String fromSocket, String to, String toSocket) {
        this(UUID.randomUUID().toString(), from, fromSocket, to, toSocket);
    }
}
