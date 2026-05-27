package com.example.gahramheit.service;

import com.example.gahramheit.dto.CommentCreateReqDTO;
import com.example.gahramheit.dto.CommentResDTO;
import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Comment;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.exception.InvalidDataException;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.AnimeRepository;
import com.example.gahramheit.repository.CommentRepository;
import com.example.gahramheit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final AnimeRepository animeRepository;

    @Transactional(readOnly = true)
    public Page<CommentResDTO> getRootComments(Long animeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return commentRepository.findByAnime_IdAndParentIdIsNull(animeId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CommentResDTO> getReplies(Long commentId) {
        Comment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (parent.getParentId() != null) {
            throw new InvalidDataException("Cannot get replies for a reply comment");
        }

        return commentRepository.findByParentId(commentId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResDTO createComment(Long userId, Long animeId, CommentCreateReqDTO request) {
        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comentario raiz no encontrado"));

            if (parent.getParentId() != null) {
                throw new InvalidDataException("No se puede replicar a una replica. Solo " +
                        "se permite un nivel de respuesta ");
            }
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Anime anime = animeRepository.findById(animeId)
                .orElseThrow(() -> new ResourceNotFoundException("Anime no encontrado"));

        Comment comment = Comment.builder()
                .anime(anime)
                .user(user)
                .content(request.getContent())
                .parentId(request.getParentId())
                .likesCount(0)
                .dislikesCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        commentRepository.save(comment);
        return toDto(comment);
    }

    public void likeComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        comment.setLikesCount(comment.getLikesCount() + 1);
        commentRepository.save(comment);
    }

    public void dislikeComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        comment.setDislikesCount(comment.getDislikesCount() + 1);
        commentRepository.save(comment);
    }

    private CommentResDTO toDto(Comment comment) {
        boolean hasReplies = false;
        if (comment.getParentId() == null) {
            hasReplies = commentRepository.countByParentId(comment.getId()) > 0;
        }

        return CommentResDTO.builder()
                .id(comment.getId())
                .animeId(comment.getAnime().getId())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .content(comment.getContent())
                .parentId(comment.getParentId())
                .likesCount(comment.getLikesCount())
                .dislikesCount(comment.getDislikesCount())
                .createdAt(comment.getCreatedAt())
                .hasReplies(hasReplies)
                .build();
    }
}
