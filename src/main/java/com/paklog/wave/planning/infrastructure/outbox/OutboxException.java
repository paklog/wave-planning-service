package com.paklog.wave.planning.infrastructure.outbox;

/**
 * Exception thrown when outbox operations fail in Wave Planning Service
 * Copied from paklog-integration to eliminate compilation dependency
 */
public class OutboxException extends RuntimeException {

    public OutboxException(String message) {
        super(message);
    }

    public OutboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
