package com.example.gahramheit.service;

import com.example.gahramheit.dto.AchievementResDTO;
import com.example.gahramheit.dto.UserProfileResDTO;
import com.example.gahramheit.dto.UserResponseDTO;
import com.example.gahramheit.dto.UserUpdateDTO;
import com.example.gahramheit.entity.Role;
import com.example.gahramheit.entity.Status;
import com.example.gahramheit.entity.User;
import com.example.gahramheit.entity.UserAnimeList;
import org.springframework.security.access.AccessDeniedException;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.repository.UserAnimeListRepository;
import com.example.gahramheit.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAnimeListRepository userAnimeListRepository;
    private final ModelMapper modelMapper;
    private final AchievementService achievementService;

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

    @Transactional
    public UserUpdateDTO updateUser(Long id, @Valid UserUpdateDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Acceso denegado. Debe estar autenticado.");
        }

        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));


        if (!Objects.equals(user.getUsername(), currentUsername) && !isAdmin) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        Optional.ofNullable(request.getUsername()).ifPresent(user::setUsername);
        Optional.ofNullable(request.getEmail()).ifPresent(user::setEmail);

        userRepository.save(user);
        return modelMapper.map(user, UserUpdateDTO.class);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public UserResponseDTO updateUserRole(Long id, Role newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setRole(newRole);
        userRepository.save(user);
        return modelMapper.map(user, UserResponseDTO.class);
    }

    private UserProfileResDTO buildProfile(User user) {
        List<UserAnimeList> userList = userAnimeListRepository.findByUser_Id(user.getId());

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
        long totalAchievements = achievementService.getUnlockedCount(user.getId());
        List<AchievementResDTO> achievements = achievementService.getUserAchievements(user.getId());
        dto.setLogrosDesbloqueados(totalAchievements + "/6");
        dto.setLogros(achievements);
        dto.setRango(calculateRango(animesCompletados));

        return dto;
    }

    private String calculateRango(long animesCompletados) {
        if (animesCompletados >= 30) return "Dios del Anime";
        if (animesCompletados >= 15) return "Otaku Experimentado";
        if (animesCompletados >= 5) return "Otaku en Formación";
        return "Nuevo en el Mundo Anime";
    }
}