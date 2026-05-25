package com.example.gahramheit.controller;

import com.example.gahramheit.exception.GlobalExceptionHandler;
import com.example.gahramheit.service.DataPopulatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeederController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SeederControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataPopulatorService dataPopulatorService;

    @Test
    void shouldRunSeederWhenEndpointIsCalled() throws Exception {
        mockMvc.perform(post("/api/admin/seed").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string("Seeder iniciado en segundo plano. Mira la consola de tu IDE."));

        verify(dataPopulatorService).populateRemainingTables();
    }

    @Test
    void shouldReturnInternalServerErrorWhenSeederFails() throws Exception {
        doThrow(new RuntimeException("seed failed")).when(dataPopulatorService).populateRemainingTables();

        mockMvc.perform(post("/api/admin/seed").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.path").value("/api/admin/seed"));
    }
}
