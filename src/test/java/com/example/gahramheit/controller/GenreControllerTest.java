package com.example.gahramheit.controller;

import com.example.gahramheit.dto.GenreDTO;
import com.example.gahramheit.exception.DuplicateResourceException;
import com.example.gahramheit.exception.GlobalExceptionHandler;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.service.GenreService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GenreController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GenreControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private GenreService genreService;

    @Test
    void shouldReturnGenresWhenGenresExist() throws Exception {
        when(genreService.getAllGenres()).thenReturn(List.of(
                new GenreDTO(1L, "Action"),
                new GenreDTO(2L, "Drama")
        ));

        mockMvc.perform(get("/api/genres").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$[0].name").value("Action"))
                .andExpect(jsonPath("$[1].name").value("Drama"));
    }

    @Test
    void shouldReturnGenreWhenGenreExists() throws Exception {
        when(genreService.getGenreById(1L)).thenReturn(new GenreDTO(1L, "Seinen"));

        mockMvc.perform(get("/api/genres/{id}", 1L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Seinen"));
    }

    @Test
    void shouldCreateGenreWhenRequestIsValid() throws Exception {
        GenreDTO request = new GenreDTO(null, "Comedy");
        when(genreService.createGenre(request)).thenReturn(new GenreDTO(3L, "Comedy"));

        mockMvc.perform(post("/api/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Comedy"));
    }

    @Test
    void shouldReturnConflictWhenGenreAlreadyExists() throws Exception {
        GenreDTO request = new GenreDTO(null, "Drama");
        when(genreService.createGenre(request)).thenThrow(new DuplicateResourceException("Genre already exists: Drama"));

        mockMvc.perform(post("/api/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Genre already exists: Drama"));
    }

    @Test
    void shouldReturnNotFoundWhenGenreDoesNotExist() throws Exception {
        when(genreService.getGenreById(99L)).thenThrow(new ResourceNotFoundException("Genre not found with id: 99"));

        mockMvc.perform(get("/api/genres/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/genres/99"));
    }

    @Test
    void shouldUpdateGenreWhenRequestIsValid() throws Exception {
        GenreDTO request = new GenreDTO(null, "Updated");
        when(genreService.updateGenre(1L, request)).thenReturn(new GenreDTO(1L, "Updated"));

        mockMvc.perform(put("/api/genres/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void shouldDeleteGenreWhenGenreExists() throws Exception {
        mockMvc.perform(delete("/api/genres/{id}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(genreService).deleteGenre(1L);
    }
}
