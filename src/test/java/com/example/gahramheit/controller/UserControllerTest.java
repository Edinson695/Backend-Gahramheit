package com.example.gahramheit.controller;

import com.example.gahramheit.dto.UserDTO;
import com.example.gahramheit.dto.UserProfileResDTO;
import com.example.gahramheit.dto.UserRecapResDTO;
import com.example.gahramheit.exception.GlobalExceptionHandler;
import com.example.gahramheit.exception.ResourceNotFoundException;
import com.example.gahramheit.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @Test
    void shouldReturnUserProfileWhenUserExists() throws Exception {
        when(userService.getUserById(1L)).thenReturn(UserProfileResDTO.builder()
                .id(1L)
                .username("john")
                .rango("Nuevo en el Mundo Anime")
                .episodiosVistos(24)
                .animesCompletados(1)
                .logrosDesbloqueados("0/6")
                .build());

        mockMvc.perform(get("/api/users/{id}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.episodiosVistos").value(24));
    }

    @Test
    void shouldReturnUserWhenUsernameExists() throws Exception {
        when(userService.getUserByUsername("john")).thenReturn(UserDTO.builder()
                .id(1L)
                .username("john")
                .email("john@gahramheit.com")
                .build());

        mockMvc.perform(get("/api/users/username/{username}", "john")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("john@gahramheit.com"));
    }

    @Test
    void shouldReturnRecapWhenUserExists() throws Exception {
        when(userService.getUserRecap(1L, 2026)).thenReturn(UserRecapResDTO.builder()
                .anio(2026)
                .totalEpisodiosVistos(50)
                .tiempoTotalMinutos(1200L)
                .generoFavorito("Action")
                .insigniaDestacadaAnual("Principiante")
                .animeMejorCalificado(UserRecapResDTO.TopAnime.builder()
                        .id(10L)
                        .title("Frieren")
                        .score(5)
                        .build())
                .build());

        mockMvc.perform(get("/api/users/{id}/recap", 1L)
                        .param("year", "2026")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(2026))
                .andExpect(jsonPath("$.animeMejorCalificado.title").value("Frieren"));
    }

    @Test
    void shouldUpdateUserWhenRequestIsValid() throws Exception {
        UserDTO request = UserDTO.builder().username("new").email("new@gahramheit.com").build();
        when(userService.updateUser(1L, request)).thenReturn(UserDTO.builder()
                .id(1L)
                .username("new")
                .email("new@gahramheit.com")
                .build());

        mockMvc.perform(put("/api/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new"));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/users/{id}", 99L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found with id: 99"))
                .andExpect(jsonPath("$.path").value("/api/users/99"));
    }

    @Test
    void shouldDeleteUserWhenUserExists() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userService).deleteUser(1L);
    }
}
