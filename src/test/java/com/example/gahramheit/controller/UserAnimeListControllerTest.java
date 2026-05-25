package com.example.gahramheit.controller;

import com.example.gahramheit.dto.AnimeStatus;
import com.example.gahramheit.dto.UpdateUserAnimeListReqDTO;
import com.example.gahramheit.dto.UserAnimeListResDTO;
import com.example.gahramheit.exception.GlobalExceptionHandler;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.service.UserAnimeListService;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAnimeListController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserAnimeListControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserAnimeListService userAnimeListService;

    @Test
    void shouldReturnUserAnimeListWhenUserExists() throws Exception {
        when(userAnimeListService.getUserList(1L)).thenReturn(List.of(UserAnimeListResDTO.builder()
                .animeId(10L)
                .title("Frieren")
                .status(AnimeStatus.Watching)
                .currentEpisode(3)
                .episodesCount(28)
                .build()));

        mockMvc.perform(get("/api/users/{userId}/anime-list", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$[0].title").value("Frieren"))
                .andExpect(jsonPath("$[0].status").value("Watching"));
    }

    @Test
    void shouldUpdateAnimeInListWhenRequestIsValid() throws Exception {
        UpdateUserAnimeListReqDTO request = new UpdateUserAnimeListReqDTO(10L, AnimeStatus.Completed, 28);
        when(userAnimeListService.updateAnimeInList(1L, request)).thenReturn(UserAnimeListResDTO.builder()
                .animeId(10L)
                .title("Frieren")
                .status(AnimeStatus.Completed)
                .currentEpisode(28)
                .episodesCount(28)
                .build());

        mockMvc.perform(put("/api/users/{userId}/anime-list", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Completed"))
                .andExpect(jsonPath("$.currentEpisode").value(28));
    }

    @Test
    void shouldReturnBadRequestWhenAnimeIdIsMissing() throws Exception {
        UpdateUserAnimeListReqDTO request = new UpdateUserAnimeListReqDTO(null, AnimeStatus.Watching, 1);

        mockMvc.perform(put("/api/users/{userId}/anime-list", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("animeId")));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        when(userAnimeListService.getUserList(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/users/{userId}/anime-list", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    @Test
    void shouldRemoveAnimeFromListWhenEntryExists() throws Exception {
        mockMvc.perform(delete("/api/users/{userId}/anime-list/{animeId}", 1L, 10L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userAnimeListService).removeFromList(1L, 10L);
    }
}
