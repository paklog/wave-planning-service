package com.paklog.wave.planning.infrastructure.outbox;

/**
 * Status of an outbox event in Wave Planning Service
 * Copied from paklog-integration to eliminate compilation dependency
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
