package br.com.formentini.eventManager.instituitiontype;

import br.com.formentini.eventManager.institutiontype.TypeInstitutionController;
import br.com.formentini.eventManager.institutiontype.TypeInstitutionService;
import br.com.formentini.eventManager.institutiontype.dto.TypeInstitutionRequest;
import br.com.formentini.eventManager.institutiontype.dto.TypeInstitutionResponse;
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
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TypeInstitutionController.class)
class TypeInstitutionControllerTest {

    private static final String BASE_URL = "/api/v1/institution-types";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TypeInstitutionService service;

    private TypeInstitutionResponse createResponse(Long id, String name) {
        return new TypeInstitutionResponse(
                id,
                name,
                OffsetDateTime.parse("2026-11-04T10:00:00-03:00")
        );
    }

    @Test
    void shouldCreateTypeInstitution() throws Exception {
        when(service.create(any(TypeInstitutionRequest.class)))
                .thenReturn(createResponse(1L, "Universidade"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Universidade"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Universidade"))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(service).create(any(TypeInstitutionRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any(TypeInstitutionRequest.class));
    }

    @Test
    void shouldReturnAllTypeInstitutions() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                createResponse(1L, "Universidade"),
                createResponse(2L, "Escola")
        ));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Universidade"))
                .andExpect(jsonPath("$[1].name").value("Escola"));

        verify(service).findAll();
    }

    @Test
    void shouldFindTypeInstitutionById() throws Exception {
        when(service.findById(1L))
                .thenReturn(createResponse(1L, "Universidade"));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Universidade"));

        verify(service).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenTypeInstitutionDoesNotExist() throws Exception {
        when(service.findById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Tipo de instituição não encontrado"
                ));

        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound());

        verify(service).findById(99L);
    }

    @Test
    void shouldUpdateTypeInstitution() throws Exception {
        when(service.update(eq(1L), any(TypeInstitutionRequest.class)))
                .thenReturn(createResponse(1L, "Faculdade"));

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Faculdade"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Faculdade"));

        verify(service).update(eq(1L), any(TypeInstitutionRequest.class));
    }

    @Test
    void shouldDeleteTypeInstitution() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonexistentTypeInstitution()
            throws Exception {

        doThrow(new ResourceNotFoundException(
                "Tipo de instituição não encontrado"
        )).when(service).delete(99L);

        mockMvc.perform(delete(BASE_URL + "/99"))
                .andExpect(status().isNotFound());

        verify(service).delete(99L);
    }
}

