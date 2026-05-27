package com.example.gahramheit.repository;

import com.example.gahramheit.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByAnime_IdAndParentIdIsNull(Long animeId, Pageable pageable);

    List<Comment> findByParentId(Long parentId);

    long countByParentId(Long parentId);
}
