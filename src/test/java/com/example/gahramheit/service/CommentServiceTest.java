package com.example.gahramheit.service;

import com.example.gahramheit.dto.CommentCreateReqDTO;
import com.example.gahramheit.dto.CommentResDTO;
import com.example.gahramheit.entity.Comment;
import com.example.gahramheit.entity.Role;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.exception.InvalidDataException;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.CommentRepository;
import com.example.gahramheit.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    void shouldReturnRootCommentsWhenAnimeHasComments() {
        Comment comment = createComment(1L, 10L, 100L, "Root", null);
        when(commentRepository.findByAnimeIdAndParentIdIsNull(10L, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(comment), PageRequest.of(0, 5), 1));
        when(userRepository.findById(100L)).thenReturn(Optional.of(createUser(100L, "john")));
        when(commentRepository.countByParentId(1L)).thenReturn(2L);

        Page<CommentResDTO> result = commentService.getRootComments(10L, 0, 5);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("john");
        assertThat(result.getContent().get(0).isHasReplies()).isTrue();
    }

    @Test
    void shouldReturnRepliesWhenParentCommentExists() {
        Comment parent = createComment(1L, 10L, 100L, "Root", null);
        Comment reply = createComment(2L, 10L, 101L, "Reply", 1L);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(commentRepository.findByParentId(1L)).thenReturn(List.of(reply));
        when(userRepository.findById(101L)).thenReturn(Optional.of(createUser(101L, "reply-user")));

        List<CommentResDTO> replies = commentService.getReplies(1L);

        assertThat(replies).hasSize(1);
        assertThat(replies.get(0).getParentId()).isEqualTo(1L);
        assertThat(replies.get(0).isHasReplies()).isFalse();
    }

    @Test
    void shouldThrowInvalidDataWhenGettingRepliesForReplyComment() {
        Comment reply = createComment(2L, 10L, 101L, "Reply", 1L);
        when(commentRepository.findById(2L)).thenReturn(Optional.of(reply));

        assertThatThrownBy(() -> commentService.getReplies(2L))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Cannot get replies for a reply comment");
    }

    @Test
    void shouldCreateRootCommentWhenRequestHasNoParent() {
        CommentCreateReqDTO request = new CommentCreateReqDTO("Hello", null);

        CommentResDTO result = commentService.createComment(100L, 10L, request);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getParentId()).isNull();
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(result.getContent()).isEqualTo("Hello");
    }

    @Test
    void shouldCreateReplyWhenParentIsRootComment() {
        Comment parent = createComment(1L, 10L, 100L, "Root", null);
        CommentCreateReqDTO request = new CommentCreateReqDTO("Reply", 1L);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(parent));

        CommentResDTO result = commentService.createComment(101L, 10L, request);

        assertThat(result.getParentId()).isEqualTo(1L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void shouldThrowInvalidDataWhenReplyingToReplyComment() {
        Comment reply = createComment(2L, 10L, 101L, "Reply", 1L);
        CommentCreateReqDTO request = new CommentCreateReqDTO("Nested reply", 2L);
        when(commentRepository.findById(2L)).thenReturn(Optional.of(reply));

        assertThatThrownBy(() -> commentService.createComment(102L, 10L, request))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Cannot reply to a reply. Only one level of replies is allowed");
    }

    @Test
    void shouldIncrementLikesWhenCommentExists() {
        Comment comment = createComment(1L, 10L, 100L, "Root", null);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        commentService.likeComment(1L);

        assertThat(comment.getLikesCount()).isEqualTo(1);
        verify(commentRepository).save(comment);
    }

    @Test
    void shouldIncrementDislikesWhenCommentExists() {
        Comment comment = createComment(1L, 10L, 100L, "Root", null);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        commentService.dislikeComment(1L);

        assertThat(comment.getDislikesCount()).isEqualTo(1);
        verify(commentRepository).save(comment);
    }

    @Test
    void shouldThrowResourceNotFoundWhenLikingMissingComment() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.likeComment(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Comment not found");
    }

    private Comment createComment(Long id, Long animeId, Long userId, String content, Long parentId) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setAnimeId(animeId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(Role.USER);
        return user;
    }
}
