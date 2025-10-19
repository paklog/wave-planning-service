package com.paklog.wms.wave.domain.repository;

import com.paklog.wms.wave.domain.aggregate.Wave;
import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStatus;
import com.paklog.wms.wave.domain.valueobject.WaveStrategy;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataMongoTest
@Import(WaveRepositoryIntegrationTest.MongoConfig.class)
class WaveRepositoryIntegrationTest {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.5");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private WaveRepository waveRepository;

    @BeforeEach
    void cleanDatabase() {
        waveRepository.deleteAll();
    }

    @Test
    void findReadyToReleaseReturnsAllocatedWaves() {
        Wave ready = plannedWave("WAVE-READY", LocalDateTime.now().minusMinutes(5));
        ready.markInventoryAllocated();
        ready.assignZone("ZONE-A");
        Wave notReady = plannedWave("WAVE-FUTURE", LocalDateTime.now().plusMinutes(10));

        waveRepository.saveAll(List.of(ready, notReady));

        List<Wave> result = waveRepository.findReadyToRelease(WaveStatus.PLANNED, LocalDateTime.now());

        assertThat(result)
                .extracting(Wave::getWaveId)
                .containsExactly("WAVE-READY");
    }

    @Test
    void findActiveWavesReturnsReleasedAndInProgress() {
        Wave released = plannedWave("WAVE-REL", LocalDateTime.now());
        released.assignZone("ZONE-A");
        released.markInventoryAllocated();
        released.release();

        Wave inProgress = plannedWave("WAVE-PROG", LocalDateTime.now());
        inProgress.assignZone("ZONE-B");
        inProgress.markInventoryAllocated();
        inProgress.release();
        inProgress.startExecution();

        Wave cancelled = plannedWave("WAVE-CAN", LocalDateTime.now());
        cancelled.cancel("No inventory");

        waveRepository.saveAll(List.of(released, inProgress, cancelled));

        List<Wave> result = waveRepository.findActiveWaves();
        assertThat(result)
                .extracting(Wave::getWaveId)
                .containsExactlyInAnyOrder("WAVE-REL", "WAVE-PROG");
    }

    @Test
    void findByOrderIdReturnsWaveContainingOrder() {
        Wave wave = plannedWave("WAVE-ORD", LocalDateTime.now());
        wave.addOrders(List.of("ORD-3"));

        waveRepository.save(wave);

        Optional<Wave> result = waveRepository.findByOrderId("ORD-3");
        assertThat(result).isPresent();
        assertThat(result.get().getWaveId()).isEqualTo("WAVE-ORD");
    }

    private Wave plannedWave(String id, LocalDateTime plannedReleaseTime) {
        Wave wave = new Wave();
        wave.setWaveId(id);
        wave.plan(
                List.of("ORD-1", "ORD-2"),
                WaveStrategy.builder()
                        .type(WaveStrategyType.TIME_BASED)
                        .maxOrders(10)
                        .timeInterval(Duration.ofMinutes(30))
                        .build(),
                "WH-1",
                WavePriority.NORMAL,
                plannedReleaseTime
        );
        return wave;
    }

    @TestConfiguration
    static class MongoConfig {
        @Bean
        MongoCustomConversions mongoCustomConversions() {
            Converter<WaveStrategy, Document> writeConverter = new Converter<>() {
                @Override
                public Document convert(WaveStrategy source) {
                    Document document = new Document();
                    document.put("type", source.getType().name());
                    document.put("maxWaveSize", source.getMaxWaveSize());
                    document.put("maxOrders", source.getMaxOrders());
                    document.put("maxLines", source.getMaxLines());
                    document.put("timeInterval", source.getTimeInterval() != null ? source.getTimeInterval().toString() : null);
                    return document;
                }
            };

            Converter<Document, WaveStrategy> readConverter = new Converter<>() {
                @Override
                public WaveStrategy convert(Document source) {
                    WaveStrategy.Builder builder = WaveStrategy.builder()
                            .type(WaveStrategyType.valueOf(source.getString("type")));
                    if (source.containsKey("maxWaveSize") && source.get("maxWaveSize") != null) {
                        builder.maxWaveSize(((Number) source.get("maxWaveSize")).intValue());
                    }
                    if (source.containsKey("maxOrders") && source.get("maxOrders") != null) {
                        builder.maxOrders(((Number) source.get("maxOrders")).intValue());
                    }
                    if (source.containsKey("maxLines") && source.get("maxLines") != null) {
                        builder.maxLines(((Number) source.get("maxLines")).intValue());
                    }
                    if (source.containsKey("timeInterval") && source.get("timeInterval") != null) {
                        builder.timeInterval(Duration.parse(source.getString("timeInterval")));
                    }
                    return builder.build();
                }
            };

            return new MongoCustomConversions(List.of(writeConverter, readConverter));
        }
    }
}
