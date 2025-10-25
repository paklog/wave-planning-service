package com.paklog.wms.wave.infrastructure.outbox;

import com.paklog.integration.outbox.OutboxEvent;
import com.paklog.integration.outbox.OutboxRepository;
import com.paklog.integration.outbox.OutboxStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB-based implementation of OutboxRepository
 * Provides outbox pattern functionality using MongoDB instead of JPA
 */
@Repository
public class MongoOutboxRepository implements OutboxRepository {

    private static final String OUTBOX_COLLECTION = "outbox_events";

    private final MongoTemplate mongoTemplate;

    public MongoOutboxRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public <S extends OutboxEvent> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(java.util.UUID.randomUUID().toString());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        mongoTemplate.save(entity, OUTBOX_COLLECTION);
        return entity;
    }

    @Override
    public <S extends OutboxEvent> List<S> saveAll(Iterable<S> entities) {
        List<S> result = new java.util.ArrayList<>();
        entities.forEach(entity -> result.add(save(entity)));
        return result;
    }

    @Override
    public Optional<OutboxEvent> findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        OutboxEvent event = mongoTemplate.findOne(query, OutboxEvent.class, OUTBOX_COLLECTION);
        return Optional.ofNullable(event);
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    @Override
    public List<OutboxEvent> findAll() {
        return mongoTemplate.findAll(OutboxEvent.class, OUTBOX_COLLECTION);
    }

    @Override
    public List<OutboxEvent> findAllById(Iterable<String> ids) {
        Query query = new Query(Criteria.where("_id").in(ids));
        return mongoTemplate.find(query, OutboxEvent.class, OUTBOX_COLLECTION);
    }

    @Override
    public long count() {
        return mongoTemplate.count(new Query(), OUTBOX_COLLECTION);
    }

    @Override
    public void deleteById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, OUTBOX_COLLECTION);
    }

    @Override
    public void delete(OutboxEvent entity) {
        deleteById(entity.getId());
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        Query query = new Query(Criteria.where("_id").in(ids));
        mongoTemplate.remove(query, OUTBOX_COLLECTION);
    }

    @Override
    public void deleteAll(Iterable<? extends OutboxEvent> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        mongoTemplate.remove(new Query(), OUTBOX_COLLECTION);
    }

    @Override
    public List<OutboxEvent> findPendingEvents(int limit) {
        Query query = new Query(Criteria.where("status").is(OutboxStatus.PENDING))
                .limit(limit);
        return mongoTemplate.find(query, OutboxEvent.class, OUTBOX_COLLECTION);
    }

    @Override
    public List<OutboxEvent> findFailedEventsForRetry(int maxRetries, int limit) {
        Query query = new Query(Criteria.where("status").is(OutboxStatus.FAILED)
                .and("retryCount").lt(maxRetries))
                .limit(limit);
        return mongoTemplate.find(query, OutboxEvent.class, OUTBOX_COLLECTION);
    }

    @Override
    public List<OutboxEvent> findByStatus(OutboxStatus status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.find(query, OutboxEvent.class, OUTBOX_COLLECTION);
    }

    @Override
    public List<OutboxEvent> findByAggregateIdOrderByCreatedAtDesc(String aggregateId) {
        Query query = new Query(Criteria.where("aggregateId").is(aggregateId))
                .with(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, OutboxEvent.class, OUTBOX_COLLECTION);
    }

    @Override
    public int deletePublishedEventsBefore(LocalDateTime dateTime) {
        Query query = new Query(Criteria.where("status").is(OutboxStatus.PUBLISHED)
                .and("publishedAt").lt(dateTime));
        return (int) mongoTemplate.remove(query, OUTBOX_COLLECTION).getDeletedCount();
    }

    @Override
    public long countByStatus(OutboxStatus status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.count(query, OUTBOX_COLLECTION);
    }

    @Override
    public void flush() {
        // MongoDB doesn't need explicit flush
    }

    @Override
    public <S extends OutboxEvent> S saveAndFlush(S entity) {
        return save(entity);
    }

    @Override
    public <S extends OutboxEvent> List<S> saveAllAndFlush(Iterable<S> entities) {
        return saveAll(entities);
    }

    @Override
    public void deleteAllInBatch(Iterable<OutboxEvent> entities) {
        deleteAll(entities);
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<String> ids) {
        deleteAllById(ids);
    }

    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }

    @Override
    public OutboxEvent getOne(String id) {
        return findById(id).orElse(null);
    }

    @Override
    public OutboxEvent getById(String id) {
        return findById(id).orElse(null);
    }

    @Override
    public OutboxEvent getReferenceById(String id) {
        return findById(id).orElse(null);
    }

    @Override
    public <S extends OutboxEvent> List<S> findAll(org.springframework.data.domain.Example<S> example) {
        throw new UnsupportedOperationException("Example queries not supported");
    }

    @Override
    public <S extends OutboxEvent> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) {
        throw new UnsupportedOperationException("Example queries not supported");
    }

    @Override
    public List<OutboxEvent> findAll(org.springframework.data.domain.Sort sort) {
        return mongoTemplate.find(new Query().with(sort), OutboxEvent.class, OUTBOX_COLLECTION);
    }

    @Override
    public org.springframework.data.domain.Page<OutboxEvent> findAll(org.springframework.data.domain.Pageable pageable) {
        Query query = new Query().with(pageable);
        List<OutboxEvent> events = mongoTemplate.find(query, OutboxEvent.class, OUTBOX_COLLECTION);
        long total = mongoTemplate.count(new Query(), OUTBOX_COLLECTION);
        return new org.springframework.data.domain.PageImpl<>(events, pageable, total);
    }

    @Override
    public <S extends OutboxEvent> Optional<S> findOne(org.springframework.data.domain.Example<S> example) {
        throw new UnsupportedOperationException("Example queries not supported");
    }

    @Override
    public <S extends OutboxEvent> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) {
        throw new UnsupportedOperationException("Example queries not supported");
    }

    @Override
    public <S extends OutboxEvent> long count(org.springframework.data.domain.Example<S> example) {
        throw new UnsupportedOperationException("Example queries not supported");
    }

    @Override
    public <S extends OutboxEvent> boolean exists(org.springframework.data.domain.Example<S> example) {
        throw new UnsupportedOperationException("Example queries not supported");
    }

    @Override
    public <S extends OutboxEvent, R> R findBy(org.springframework.data.domain.Example<S> example,
                                                 java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        throw new UnsupportedOperationException("Fluent queries not supported");
    }
}
