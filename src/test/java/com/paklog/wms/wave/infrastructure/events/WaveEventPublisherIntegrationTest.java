package com.paklog.wms.wave.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paklog.wms.wave.domain.event.WavePlannedEvent;
import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class WaveEventPublisherIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.3")
                    .asCompatibleSubstituteFor("confluentinc/cp-kafka")
    );

    private KafkaTemplate<String, Object> kafkaTemplate;
    private Consumer<String, LinkedHashMap<String, Object>> consumer;
    private WaveEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(mapper);
        jsonSerializer.setAddTypeInfo(false);

        DefaultKafkaProducerFactory<String, Object> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps(), new StringSerializer(), jsonSerializer);

        kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.setDefaultTopic("wms-wave-events");
        publisher = new WaveEventPublisher(kafkaTemplate, "wms-wave-events", "/integration-test");

        JsonDeserializer<LinkedHashMap<String, Object>> valueDeserializer =
                new JsonDeserializer<>(LinkedHashMap.class, mapper, false);
        valueDeserializer.addTrustedPackages("*");

        DefaultKafkaConsumerFactory<String, LinkedHashMap<String, Object>> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps(), new StringDeserializer(), valueDeserializer);
        consumer = consumerFactory.createConsumer("integration-group", "integration-client");
        consumer.subscribe(List.of("wms-wave-events"));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void publishEventWritesMessageToKafkaTopic() {
        WavePlannedEvent event = new WavePlannedEvent(
                "WAVE-KAFKA",
                List.of("ORD-1"),
                "WH-1",
                WaveStrategyType.TIME_BASED,
                WavePriority.NORMAL,
                LocalDateTime.now()
        );

        publisher.publishEvent(event);

        ConsumerRecords<String, LinkedHashMap<String, Object>> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        LinkedHashMap<String, Object> payload = records.iterator().next().value();
        assertThat(payload.get("type")).isEqualTo("com.paklog.wms.wave.wave.planned.v1");
        assertThat(payload.get("source")).isEqualTo("/integration-test");
        assertThat(payload.get("id")).isNotNull();
    }

    private Map<String, Object> producerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        return props;
    }

    private Map<String, Object> consumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "wave-service-test");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        return props;
    }
}
