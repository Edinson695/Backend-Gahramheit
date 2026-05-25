package com.example.gahramheit.controller;

import com.example.gahramheit.dto.EpisodeDTO;
import com.example.gahramheit.exception.GlobalExceptionHandler;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.service.EpisodeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EpisodeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EpisodeControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private EpisodeService episodeService;

    @Test
    void shouldReturnEpisodesWhenAnimeExists() throws Exception {
        when(episodeService.getEpisodesByAnime(10L)).thenReturn(List.of(
                EpisodeDTO.builder().id(1L).animeId(10L).episodeNumber(1).title("Start").build(),
                EpisodeDTO.builder().id(2L).animeId(10L).episodeNumber(2).title("Next").build()
        ));

        mockMvc.perform(get("/api/anime/{animeId}/episodes", 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$[0].episodeNumber").value(1))
                .andExpect(jsonPath("$[1].title").value("Next"));
    }

    @Test
    void shouldReturnEpisodeWhenEpisodeExists() throws Exception {
        when(episodeService.getEpisodeById(1L)).thenReturn(EpisodeDTO.builder()
                .id(1L)
                .animeId(10L)
                .episodeNumber(1)
                .title("Start")
                .build());

        mockMvc.perform(get("/api/episodes/{id}", 1L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animeId").value(10))
                .andExpect(jsonPath("$.title").value("Start"));
    }

    @Test
    void shouldCreateEpisodeWhenRequestIsValid() throws Exception {
        EpisodeDTO request = EpisodeDTO.builder().episodeNumber(1).title("Start").build();
        when(episodeService.createEpisode(10L, request)).thenReturn(EpisodeDTO.builder()
                .id(1L)
                .animeId(10L)
                .episodeNumber(1)
                .title("Start")
                .build());

        mockMvc.perform(post("/api/anime/{animeId}/episodes", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.animeId").value(10));
    }

    @Test
    void shouldReturnNotFoundWhenEpisodeDoesNotExist() throws Exception {
        when(episodeService.getEpisodeById(99L)).thenThrow(new ResourceNotFoundException("Episode not found with id: 99"));

        mockMvc.perform(get("/api/episodes/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/episodes/99"));
    }
}
