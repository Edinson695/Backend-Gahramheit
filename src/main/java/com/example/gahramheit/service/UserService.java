package com.example.gahramheit.service;

import com.example.gahramheit.dto.UserProfileResDTO;
import com.example.gahramheit.dto.UserRecapResDTO;
import com.example.gahramheit.dto.UserResponseDTO;
import com.example.gahramheit.dto.UserUpdateDTO;
import com.example.gahramheit.entity.Role;
import com.example.gahramheit.entity.Status;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.entity.UserAnimeList;
import com.example.gahramheit.exception.AccessDeniedException;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.UserAnimeListRepository;
import com.example.gahramheit.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAnimeListRepository userAnimeListRepository;
    private final ModelMapper modelMapper;

    public UserProfileResDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return buildProfile(user);
    }

    public UserResponseDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return modelMapper.map(user, UserResponseDTO.class);
    }

    public UserProfileResDTO getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return buildProfile(user);
    }

    public UserRecapResDTO getUserRecap(Long id, Integer year) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        List<UserAnimeList> userList = userAnimeListRepository.findByUserId(id);

        int totalEpisodes = userList.stream()
                .filter(ual -> ual.getCurrentEpisode() != null)
                .mapToInt(UserAnimeList::getCurrentEpisode)
                .sum();

        long completedCount = userList.stream()
                .filter(ual -> ual.getStatus() == Status.COMPLETED)
                .count();

        UserRecapResDTO dto = new UserRecapResDTO();
        dto.setAnio(year);
        dto.setTotalEpisodiosVistos(totalEpisodes);
        dto.setTiempoTotalMinutos(totalEpisodes * 24L);
        dto.setGeneroFavorito("Sin datos");
        dto.setInsigniaDestacadaAnual(completedCount >= 10 ? "Completador Serial" : "Principiante");

        UserRecapResDTO.TopAnime top = new UserRecapResDTO.TopAnime();
        top.setId(0L);
        top.setTitle("Sin datos");
        top.setScore(0);
        dto.setAnimeMejorCalificado(top);

        return dto;
    }

    public UserUpdateDTO updateUser(Long id, @Valid UserUpdateDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!user.getUsername().equals(currentUsername) && !isAdmin) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        Optional.ofNullable(request.getUsername()).ifPresent(user::setUsername);
        Optional.ofNullable(request.getEmail()).ifPresent(user::setEmail);

        userRepository.save(user);
        return modelMapper.map(user, UserUpdateDTO.class);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
    }

    public UserResponseDTO updateUserRole(Long id, Role newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setRole(newRole);
        userRepository.save(user);
        return modelMapper.map(user, UserResponseDTO.class);
    }

    private UserProfileResDTO buildProfile(User user) {
        List<UserAnimeList> userList = userAnimeListRepository.findByUserId(user.getId());

        int episodiosVistos = userList.stream()
                .filter(ual -> ual.getCurrentEpisode() != null)
                .mapToInt(UserAnimeList::getCurrentEpisode)
                .sum();

        long animesCompletados = userList.stream()
                .filter(ual -> ual.getStatus() == Status.COMPLETED)
                .count();

        UserProfileResDTO dto = new UserProfileResDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole().name());
        dto.setEpisodiosVistos(episodiosVistos);
        dto.setAnimesCompletados((int) animesCompletados);
        dto.setLogrosDesbloqueados("0/6");

        if (animesCompletados >= 30) {
            dto.setRango("Dios del Anime");
        } else if (animesCompletados >= 15) {
            dto.setRango("Otaku Experimentado");
        } else if (animesCompletados >= 5) {
            dto.setRango("Otaku en Formación");
        } else {
            dto.setRango("Nuevo en el Mundo Anime");
        }

        return dto;
    }
}
