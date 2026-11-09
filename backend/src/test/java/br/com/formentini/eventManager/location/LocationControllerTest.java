package br.com.formentini.eventManager.location;

import br.com.formentini.eventManager.location.dto.LocationRequest;
import br.com.formentini.eventManager.location.dto.LocationResponse;
import br.com.formentini.eventManager.shared.exception.GlobalExceptionHandler;
import br.com.formentini.eventManager.shared.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LocationController.class)
@Import(GlobalExceptionHandler.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LocationService locationService;

    private LocationRequest validRequest() {
        return new LocationRequest(
                "Auditório Principal",
                1L
        );
    }

    private LocationResponse response() {
        return new LocationResponse(
                1L,
                "Auditório Principal",
                1L,
                "Universidade Estadual",
                null
        );
    }

    @Test
    void shouldCreateLocation() throws Exception {
        when(locationService.create(any()))
                .thenReturn(response());

        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Auditório Principal"))
                .andExpect(jsonPath("$.institutionId").value(1));
    }

    @Test
    void shouldRejectLocationWithoutName() throws Exception {
        LocationRequest request = new LocationRequest(
                " ",
                1L
        );

        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(locationService, never()).create(any());
    }

    @Test
    void shouldRejectLocationWithoutInstitution() throws Exception {
        LocationRequest request = new LocationRequest(
                "Auditório",
                null
        );

        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllLocations() throws Exception {
        when(locationService.findAll())
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name")
                        .value("Auditório Principal"));
    }

    @Test
    void shouldFindLocationById() throws Exception {
        when(locationService.findById(1L))
                .thenReturn(response());

        mockMvc.perform(get("/api/v1/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldReturnNotFoundWhenLocationDoesNotExist() throws Exception {
        when(locationService.findById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Local não encontrado com ID: 99"
                ));

        mockMvc.perform(get("/api/v1/locations/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFindLocationsByInstitution() throws Exception {
        when(locationService.findByInstitution(1L))
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/locations/institution/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldUpdateLocation() throws Exception {
        LocationResponse updated = new LocationResponse(
                1L,
                "Novo Auditório",
                1L,
                "Universidade Estadual",
                null
        );

        when(locationService.update(eq(1L), any()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LocationRequest(
                                        "Novo Auditório",
                                        1L
                                )
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novo Auditório"));
    }

    @Test
    void shouldDeleteLocation() throws Exception {
        doNothing().when(locationService).delete(1L);

        mockMvc.perform(delete("/api/v1/locations/1"))
                .andExpect(status().isNoContent());

        verify(locationService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingLocation()
            throws Exception {

        doThrow(new ResourceNotFoundException(
                "Local não encontrado com ID: 99"
        )).when(locationService).delete(99L);

        mockMvc.perform(delete("/api/v1/locations/99"))
                .andExpect(status().isNotFound());
    }
}
