package com.example.gahramheit.controller;
import com.example.gahramheit.dto.AnimeDTO;
import com.example.gahramheit.dto.AnimeDetailResDTO;
import com.example.gahramheit.entity.Anime;
import com.example.gahramheit.entity.Genre;
import com.example.gahramheit.service.AnimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anime")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AnimeController {
    private final AnimeService animeService;
    @GetMapping
    public ResponseEntity<Page<AnimeDTO>>   getAnimeCatalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(animeService.getAnimeCatalog(page, size));
    }
    @GetMapping("/search")
    public ResponseEntity<List<AnimeDTO>> searchAnimesByTitle(
            @RequestParam String keyword) {
        return ResponseEntity.ok(animeService.searchAnimesByTitle(keyword));
    }
    @GetMapping("/{id}")
    public ResponseEntity<AnimeDetailResDTO> getAnimeDetails(@PathVariable Long id) {
        return ResponseEntity.ok(animeService.getAnimeDetails(id));
    }

    @GetMapping("/genre/name/{name}")
    public ResponseEntity<Page<AnimeDTO>> findByGenreName(
            @PathVariable String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                animeService.animeFiltradoPorNombre(name, page, size)
        );
    }
    //request dar anime por genero
}