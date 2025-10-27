package com.paklog.wave.planning.infrastructure.outbox;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Repository interface for managing outbox events in Wave Planning Service
 * Copied from paklog-integration to eliminate compilation dependency
 * Implemented by MongoOutboxRepository for MongoDB storage
 */
public interface OutboxRepository {

    /**
     * Save an outbox event
     */
    <S extends OutboxEvent> S save(S entity);

    /**
     * Save multiple outbox events
     */
    <S extends OutboxEvent> List<S> saveAll(Iterable<S> entities);

    /**
     * Find event by ID
     */
    Optional<OutboxEvent> findById(String id);

    /**
     * Check if event exists by ID
     */
    boolean existsById(String id);

    /**
     * Find all events
     */
    List<OutboxEvent> findAll();

    /**
     * Find events by IDs
     */
    List<OutboxEvent> findAllById(Iterable<String> ids);

    /**
     * Count all events
     */
    long count();

    /**
     * Delete event by ID
     */
    void deleteById(String id);

    /**
     * Delete event
     */
    void delete(OutboxEvent entity);

    /**
     * Delete events by IDs
     */
    void deleteAllById(Iterable<? extends String> ids);

    /**
     * Delete multiple events
     */
    void deleteAll(Iterable<? extends OutboxEvent> entities);

    /**
     * Delete all events
     */
    void deleteAll();

    /**
     * Find pending events ordered by creation time
     */
    List<OutboxEvent> findPendingEvents(int limit);

    /**
     * Find failed events that should be retried
     */
    List<OutboxEvent> findFailedEventsForRetry(int maxRetries, int limit);

    /**
     * Find all events by status
     */
    List<OutboxEvent> findByStatus(OutboxStatus status);

    /**
     * Find events by aggregate ID
     */
    List<OutboxEvent> findByAggregateIdOrderByCreatedAtDesc(String aggregateId);

    /**
     * Delete published events older than specified date
     */
    int deletePublishedEventsBefore(LocalDateTime dateTime);

    /**
     * Count events by status
     */
    long countByStatus(OutboxStatus status);

    // Repository compatibility methods
    void flush();
    <S extends OutboxEvent> S saveAndFlush(S entity);
    <S extends OutboxEvent> List<S> saveAllAndFlush(Iterable<S> entities);
    void deleteAllInBatch(Iterable<OutboxEvent> entities);
    void deleteAllByIdInBatch(Iterable<String> ids);
    void deleteAllInBatch();
    OutboxEvent getOne(String id);
    OutboxEvent getById(String id);
    OutboxEvent getReferenceById(String id);
    <S extends OutboxEvent> List<S> findAll(Example<S> example);
    <S extends OutboxEvent> List<S> findAll(Example<S> example, Sort sort);
    List<OutboxEvent> findAll(Sort sort);
    Page<OutboxEvent> findAll(Pageable pageable);
    <S extends OutboxEvent> Optional<S> findOne(Example<S> example);
    <S extends OutboxEvent> Page<S> findAll(Example<S> example, Pageable pageable);
    <S extends OutboxEvent> long count(Example<S> example);
    <S extends OutboxEvent> boolean exists(Example<S> example);
    <S extends OutboxEvent, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction);
}
