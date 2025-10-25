package com.paklog.wms.wave.infrastructure.resilience;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WarehouseOperationsClientTest {

    private MockWebServer server;
    private WarehouseOperationsClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new WarehouseOperationsClient(server.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void planWaveReturnsResponsePayload() {
        server.enqueue(new MockResponse()
                .setBody("{\"status\":\"ok\"}")
                .addHeader("Content-Type", "application/json"));

        Object response = client.planWave("WAVE-1");

        assertThat(response).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) response).get("status")).isEqualTo("ok");
    }

    @Test
    void planWaveThrowsLegacyExceptionOnFailure() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> client.planWave("WAVE-ERR"))
                .isInstanceOf(WarehouseOperationsClient.LegacySystemException.class)
                .hasMessageContaining("Failed to call legacy system");
    }

    @Test
    void getWaveRetrievesWavePayload() {
        server.enqueue(new MockResponse()
                .setBody("{\"waveId\":\"WAVE-2\"}")
                .addHeader("Content-Type", "application/json"));

        Object response = client.getWave("WAVE-2");

        assertThat(response).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) response).get("waveId")).isEqualTo("WAVE-2");
    }
}

