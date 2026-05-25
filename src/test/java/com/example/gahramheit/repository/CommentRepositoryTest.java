package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Comment;
import com.example.gahramheit.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void shouldSaveCommentWhenCommentIsValid() {
        Comment comment = createComment(1L, 10L, "Great episode", null);

        Comment savedComment = commentRepository.saveAndFlush(comment);

        assertThat(savedComment.getId()).isNotNull();
        assertThat(savedComment.getLikesCount()).isZero();
        assertThat(savedComment.getDislikesCount()).isZero();
    }

    @Test
    void shouldFindRootCommentsWhenAnimeExists() {
        Comment rootComment = commentRepository.saveAndFlush(createComment(1L, 10L, "Root comment", null));
        commentRepository.saveAndFlush(createComment(1L, 11L, "Reply comment", rootComment.getId()));
        commentRepository.saveAndFlush(createComment(2L, 10L, "Other anime comment", null));

        Page<Comment> comments = commentRepository.findByAnimeIdAndParentIdIsNull(1L, PageRequest.of(0, 10));

        assertThat(comments.getContent())
                .extracting(Comment::getContent)
                .containsExactly("Root comment");
    }

    @Test
    void shouldFindRepliesWhenParentCommentExists() {
        Comment rootComment = commentRepository.saveAndFlush(createComment(1L, 10L, "Root comment", null));
        commentRepository.saveAndFlush(createComment(1L, 11L, "First reply", rootComment.getId()));
        commentRepository.saveAndFlush(createComment(1L, 12L, "Second reply", rootComment.getId()));

        List<Comment> replies = commentRepository.findByParentId(rootComment.getId());

        assertThat(replies)
                .extracting(Comment::getContent)
                .containsExactlyInAnyOrder("First reply", "Second reply");
    }

    @Test
    void shouldCountRepliesWhenParentCommentExists() {
        Comment rootComment = commentRepository.saveAndFlush(createComment(1L, 10L, "Root comment", null));
        commentRepository.saveAndFlush(createComment(1L, 11L, "First reply", rootComment.getId()));
        commentRepository.saveAndFlush(createComment(1L, 12L, "Second reply", rootComment.getId()));

        long repliesCount = commentRepository.countByParentId(rootComment.getId());

        assertThat(repliesCount).isEqualTo(2);
    }

    @Test
    void shouldRejectCommentWhenContentIsMissing() {
        Comment comment = createComment(1L, 10L, null, null);

        assertThatThrownBy(() -> commentRepository.saveAndFlush(comment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectCommentWhenCreatedAtIsMissing() {
        Comment comment = createComment(1L, 10L, "Missing date", null);
        comment.setCreatedAt(null);

        assertThatThrownBy(() -> commentRepository.saveAndFlush(comment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Comment createComment(Long animeId, Long userId, String content, Long parentId) {
        Comment comment = new Comment();
        comment.setAnimeId(animeId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }
}
