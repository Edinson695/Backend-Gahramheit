package com.example.gahramheit.controller;

import com.example.gahramheit.dto.CommentCreateReqDTO;
import com.example.gahramheit.dto.CommentResDTO;
import com.example.gahramheit.exception.GlobalExceptionHandler;
import com.example.gahramheit.exception.InvalidDataException;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CommentControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CommentService commentService;

    @Test
    void shouldReturnRootCommentsWhenAnimeHasComments() throws Exception {
        when(commentService.getRootComments(10L, 0, 5)).thenReturn(new PageImpl<>(
                List.of(createCommentResponse(1L, 10L, 100L, "Root", null, true)),
                PageRequest.of(0, 5),
                1
        ));

        mockMvc.perform(get("/api/anime/{animeId}/comments", 10L)
                        .param("page", "0")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.content[0].content").value("Root"))
                .andExpect(jsonPath("$.content[0].hasReplies").value(true));
    }

    @Test
    void shouldReturnRepliesWhenParentCommentExists() throws Exception {
        when(commentService.getReplies(1L)).thenReturn(List.of(
                createCommentResponse(2L, 10L, 101L, "Reply", 1L, false)
        ));

        mockMvc.perform(get("/api/comments/{commentId}/replies", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].parentId").value(1))
                .andExpect(jsonPath("$[0].content").value("Reply"));
    }

    @Test
    void shouldCreateCommentWhenRequestIsValid() throws Exception {
        CommentCreateReqDTO request = new CommentCreateReqDTO("Hello", null);
        when(commentService.createComment(100L, 10L, request))
                .thenReturn(createCommentResponse(1L, 10L, 100L, "Hello", null, false));

        mockMvc.perform(post("/api/anime/{animeId}/comments", 10L)
                        .param("userId", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hello"))
                .andExpect(jsonPath("$.userId").value(100));
    }

    @Test
    void shouldReturnBadRequestWhenCommentContentIsBlank() throws Exception {
        CommentCreateReqDTO request = new CommentCreateReqDTO("", null);

        mockMvc.perform(post("/api/anime/{animeId}/comments", 10L)
                        .param("userId", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("content")));
    }

    @Test
    void shouldReturnBadRequestWhenGettingRepliesForReplyComment() throws Exception {
        when(commentService.getReplies(2L)).thenThrow(new InvalidDataException("Cannot get replies for a reply comment"));

        mockMvc.perform(get("/api/comments/{commentId}/replies", 2L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot get replies for a reply comment"));
    }

    @Test
    void shouldReturnNotFoundWhenCommentDoesNotExist() throws Exception {
        when(commentService.getReplies(99L)).thenThrow(new ResourceNotFoundException("Comment not found with id: 99"));

        mockMvc.perform(get("/api/comments/{commentId}/replies", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/comments/99/replies"));
    }

    @Test
    void shouldLikeCommentWhenCommentExists() throws Exception {
        mockMvc.perform(put("/api/comments/{commentId}/like", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(commentService).likeComment(1L);
    }

    @Test
    void shouldDislikeCommentWhenCommentExists() throws Exception {
        mockMvc.perform(put("/api/comments/{commentId}/dislike", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(commentService).dislikeComment(1L);
    }

    private CommentResDTO createCommentResponse(Long id, Long animeId, Long userId, String content, Long parentId, boolean hasReplies) {
        return CommentResDTO.builder()
                .id(id)
                .animeId(animeId)
                .userId(userId)
                .username("john")
                .content(content)
                .parentId(parentId)
                .likesCount(1)
                .dislikesCount(0)
                .createdAt(LocalDateTime.now())
                .hasReplies(hasReplies)
                .build();
    }
}
