package com.paklog.wms.wave.adapter.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.wms.wave.adapter.rest.dto.CreateWaveRequest;
import com.paklog.wms.wave.adapter.rest.dto.ReleaseWaveRequest;
import com.paklog.wms.wave.application.command.CreateWaveCommand;
import com.paklog.wms.wave.application.service.WavePlanningService;
import com.paklog.wms.wave.application.service.WavePlanningService.WaveNotFoundException;
import com.paklog.wms.wave.domain.aggregate.Wave;
import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStatus;
import com.paklog.wms.wave.domain.valueobject.WaveStrategy;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WaveController.class)
class WaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WavePlanningService wavePlanningService;

    private Wave sampleWave;

    @BeforeEach
    void setUp() {
        sampleWave = new Wave();
        sampleWave.setWaveId("WAVE-123");
        sampleWave.plan(
                List.of("ORD-1", "ORD-2"),
                WaveStrategy.builder()
                        .type(WaveStrategyType.TIME_BASED)
                        .maxOrders(10)
                        .timeInterval(Duration.ofMinutes(30))
                        .build(),
                "WH-1",
                WavePriority.HIGH,
                LocalDateTime.now().plusHours(1)
        );
        sampleWave.assignZone("ZONE-1");
        sampleWave.markInventoryAllocated();
    }

    @Test
    void createWaveReturnsCreatedWave() throws Exception {
        when(wavePlanningService.createWave(any(CreateWaveCommand.class))).thenReturn(sampleWave);

        CreateWaveRequest request = new CreateWaveRequest(
                List.of("ORD-1", "ORD-2"),
                WaveStrategyType.TIME_BASED,
                "WH-1",
                WavePriority.HIGH,
                LocalDateTime.now().plusHours(1),
                50,
                100,
                "PT30M"
        );

        mockMvc.perform(post("/api/v1/waves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.waveId").value("WAVE-123"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.orderCount").value(2));

        ArgumentCaptor<CreateWaveCommand> commandCaptor = ArgumentCaptor.forClass(CreateWaveCommand.class);
        verify(wavePlanningService).createWave(commandCaptor.capture());
        assertThat(commandCaptor.getValue().timeInterval()).isEqualTo(Duration.parse("PT30M"));
    }

    @Test
    void createWaveValidationErrorsReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/waves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderIds": [],
                                  "warehouseId": "",
                                  "strategy": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWaveReturnsWave() throws Exception {
        when(wavePlanningService.findWaveById("WAVE-123")).thenReturn(sampleWave);

        mockMvc.perform(get("/api/v1/waves/WAVE-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waveId").value("WAVE-123"));
    }

    @Test
    void getWaveNotFoundReturns404() throws Exception {
        when(wavePlanningService.findWaveById("WAVE-404")).thenThrow(new WaveNotFoundException("WAVE-404"));

        mockMvc.perform(get("/api/v1/waves/WAVE-404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listWavesDelegatesToService() throws Exception {
        when(wavePlanningService.findActiveWaves()).thenReturn(List.of(sampleWave));

        mockMvc.perform(get("/api/v1/waves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].waveId").value("WAVE-123"));

        verify(wavePlanningService).findActiveWaves();

        when(wavePlanningService.findWavesByWarehouseAndStatus("WH-1", WaveStatus.PLANNED))
                .thenReturn(List.of(sampleWave));

        mockMvc.perform(get("/api/v1/waves")
                        .param("status", "PLANNED")
                        .param("warehouseId", "WH-1"))
                .andExpect(status().isOk());

        verify(wavePlanningService).findWavesByWarehouseAndStatus("WH-1", WaveStatus.PLANNED);
    }

    @Test
    void releaseWaveReturnsUpdatedWave() throws Exception {
        when(wavePlanningService.releaseWave(any())).thenReturn(sampleWave);

        mockMvc.perform(post("/api/v1/waves/WAVE-123/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReleaseWaveRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waveId").value("WAVE-123"));
    }

    @Test
    void releaseWaveConflictReturns409() throws Exception {
        when(wavePlanningService.releaseWave(any())).thenThrow(new IllegalStateException("Cannot release"));

        mockMvc.perform(post("/api/v1/waves/WAVE-123/release"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelWaveReturnsWave() throws Exception {
        when(wavePlanningService.cancelWave(any())).thenReturn(sampleWave);

        mockMvc.perform(post("/api/v1/waves/WAVE-123/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Customer request"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waveId").value("WAVE-123"));
    }

    @Test
    void assignZoneReturnsWave() throws Exception {
        when(wavePlanningService.assignZone(any())).thenReturn(sampleWave);

        mockMvc.perform(post("/api/v1/waves/WAVE-123/assign-zone")
                        .param("zone", "A1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waveId").value("WAVE-123"));
    }

    @Test
    void readyToReleaseEndpointReturnsList() throws Exception {
        when(wavePlanningService.findWavesReadyToRelease()).thenReturn(List.of(sampleWave));

        mockMvc.perform(get("/api/v1/waves/ready-to-release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].waveId").value("WAVE-123"));
    }
}
