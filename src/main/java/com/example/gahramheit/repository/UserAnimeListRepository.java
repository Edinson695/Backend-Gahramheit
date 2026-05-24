package com.example.gahramheit.repository;

import com.example.proyec_back.entity.UserAnimeList;
import com.example.proyec_back.entity.UserAnimeListId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAnimeListRepository extends JpaRepository<UserAnimeList, UserAnimeListId> {
}

