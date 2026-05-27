package com.example.gahramheit.repository;

import com.example.gahramheit.entity.UserAnimeList;
import com.example.gahramheit.entity.UserAnimeListId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAnimeListRepository extends JpaRepository<UserAnimeList, UserAnimeListId> {
    List<UserAnimeList> findByUser_Id(Long userId);
}

