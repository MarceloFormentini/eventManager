package br.com.formentini.eventManager.institution;

import br.com.formentini.eventManager.institution.dto.InstitutionRequest;
import br.com.formentini.eventManager.institution.dto.InstitutionResponse;
import br.com.formentini.eventManager.shared.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InstitutionController.class)
class InstitutionControllerTest {

    private static final String BASE_URL = "/api/v1/institutions";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstitutionService service;

    private InstitutionResponse createResponse(
            Long id,
            String name,
            Long typeId,
            String typeName) {

        return new InstitutionResponse(
                id,
                name,
                typeId,
                typeName,
                OffsetDateTime.parse("2026-11-06T10:00:00-03:00")
        );
    }

    @Test
    void shouldCreateInstitution() throws Exception {
        when(service.create(any(InstitutionRequest.class)))
                .thenReturn(createResponse(
                        1L, "Universidade Estadual", 1L, "Universidade"
                ));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Universidade Estadual",
                                    "typeInstitutionId": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Universidade Estadual"))
                .andExpect(jsonPath("$.typeInstitutionId").value(1))
                .andExpect(jsonPath("$.typeInstitutionName").value("Universidade"));

        verify(service).create(any(InstitutionRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "",
                                    "typeInstitutionId": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any(InstitutionRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenTypeInstitutionIdIsNull() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Universidade Estadual",
                                    "typeInstitutionId": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any(InstitutionRequest.class));
    }

    @Test
    void shouldReturnAllInstitutions() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                createResponse(1L, "Universidade Estadual", 1L, "Universidade"),
                createResponse(2L, "Escola Municipal", 2L, "Escola")
        ));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Universidade Estadual"))
                .andExpect(jsonPath("$[1].name").value("Escola Municipal"));

        verify(service).findAll();
    }

    @Test
    void shouldFindInstitutionById() throws Exception {
        when(service.findById(1L))
                .thenReturn(createResponse(
                        1L, "Universidade Estadual", 1L, "Universidade"
                ));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Universidade Estadual"));

        verify(service).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenInstitutionDoesNotExist() throws Exception {
        when(service.findById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Instituição não encontrada: 99"
                ));

        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound());

        verify(service).findById(99L);
    }

    @Test
    void shouldFindInstitutionsByType() throws Exception {
        when(service.findByTypeInstitution(1L))
                .thenReturn(List.of(
                        createResponse(
                                1L, "Universidade Estadual", 1L, "Universidade"
                        )
                ));

        mockMvc.perform(get(BASE_URL + "/type/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].typeInstitutionId").value(1));

        verify(service).findByTypeInstitution(1L);
    }

    @Test
    void shouldReturnNotFoundWhenTypeDoesNotExist() throws Exception {
        when(service.findByTypeInstitution(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Tipo de instituição não encontrado: 99"
                ));

        mockMvc.perform(get(BASE_URL + "/type/99"))
                .andExpect(status().isNotFound());

        verify(service).findByTypeInstitution(99L);
    }

    @Test
    void shouldUpdateInstitution() throws Exception {
        when(service.update(eq(1L), any(InstitutionRequest.class)))
                .thenReturn(createResponse(
                        1L, "Universidade Federal", 1L, "Universidade"
                ));

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Universidade Federal",
                                    "typeInstitutionId": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Universidade Federal"));

        verify(service).update(eq(1L), any(InstitutionRequest.class));
    }

    @Test
    void shouldDeleteInstitution() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonexistentInstitution()
            throws Exception {

        doThrow(new ResourceNotFoundException(
                "Instituição não encontrada: 99"
        )).when(service).delete(99L);

        mockMvc.perform(delete(BASE_URL + "/99"))
                .andExpect(status().isNotFound());

        verify(service).delete(99L);
    }
}
