package com.paklog.wms.wave.support;

import com.paklog.wms.wave.domain.valueobject.WaveStrategy;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;
import org.bson.Document;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.Duration;
import java.util.List;

/**
 * Shared Mongo custom conversions for tests using Testcontainers.
 */
@TestConfiguration
public class TestMongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
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

