package com.example.gahramheit.controller;

import com.example.gahramheit.dto.ReviewCreateReqDTO;
import com.example.gahramheit.dto.ReviewResDTO;
import com.example.gahramheit.exception.GlobalExceptionHandler;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReviewControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ReviewService reviewService;

    @Test
    void shouldCreateReviewWhenRequestIsValid() throws Exception {
        ReviewCreateReqDTO request = new ReviewCreateReqDTO(10L, 5, "Great");
        when(reviewService.createReview(1L, request)).thenReturn(ReviewResDTO.builder()
                .id(100L)
                .username("john")
                .score(5)
                .comment("Great")
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/reviews")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Type", startsWith(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    void shouldReturnBadRequestWhenReviewAnimeIdIsMissing() throws Exception {
        ReviewCreateReqDTO request = new ReviewCreateReqDTO(null, 5, "Great");

        mockMvc.perform(post("/reviews")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("animeId")));
    }

    @Test
    void shouldReturnReviewsWhenAnimeExists() throws Exception {
        when(reviewService.getReviewsByAnime(10L)).thenReturn(List.of(
                ReviewResDTO.builder().id(1L).username("john").score(5).comment("Great").build(),
                ReviewResDTO.builder().id(2L).username("jane").score(4).comment("Nice").build()
        ));

        mockMvc.perform(get("/anime/{animeId}/reviews", 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("john"))
                .andExpect(jsonPath("$[1].score").value(4));
    }

    @Test
    void shouldReturnReviewWhenReviewExists() throws Exception {
        when(reviewService.getReviewById(1L)).thenReturn(ReviewResDTO.builder()
                .id(1L)
                .username("john")
                .score(5)
                .comment("Great")
                .build());

        mockMvc.perform(get("/reviews/{id}", 1L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.comment").value("Great"));
    }

    @Test
    void shouldReturnNotFoundWhenReviewDoesNotExist() throws Exception {
        when(reviewService.getReviewById(99L)).thenThrow(new ResourceNotFoundException("Review not found with id: 99"));

        mockMvc.perform(get("/reviews/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Review not found with id: 99"));
    }

    @Test
    void shouldDeleteReviewWhenReviewExists() throws Exception {
        mockMvc.perform(delete("/reviews/{id}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(reviewService).deleteReview(1L);
    }
}
