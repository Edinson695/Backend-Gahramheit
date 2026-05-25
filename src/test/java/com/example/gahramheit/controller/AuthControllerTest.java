package com.example.gahramheit.controller;

import com.example.gahramheit.dto.AuthResDTO;
import com.example.gahramheit.dto.UserLoginReqDTO;
import com.example.gahramheit.dto.UserRegisterReqDTO;
import com.example.gahramheit.exception.DuplicateResourceException;
import com.example.gahramheit.exception.GlobalExceptionHandler;
import com.example.gahramheit.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @Test
    void shouldLoginWhenCredentialsAreValid() throws Exception {
        UserLoginReqDTO request = new UserLoginReqDTO("john", "password123");
        when(authService.login(request)).thenReturn(createAuthResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.username").value("john"));

        verify(authService).login(request);
    }

    @Test
    void shouldReturnCreatedWhenRegisterRequestIsValid() throws Exception {
        UserRegisterReqDTO request = new UserRegisterReqDTO("john", "john@gahramheit.com", "password123");
        when(authService.register(request)).thenReturn(createAuthResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("john@gahramheit.com"));

        verify(authService).register(request);
    }

    @Test
    void shouldReturnConflictWhenUsernameAlreadyExists() throws Exception {
        UserRegisterReqDTO request = new UserRegisterReqDTO("john", "john@gahramheit.com", "password123");
        when(authService.register(request))
                .thenThrow(new DuplicateResourceException("Username already in use: john"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username already in use: john"));
    }

    @Test
    void shouldReturnBadRequestWhenRegisterRequestIsInvalid() throws Exception {
        UserRegisterReqDTO request = new UserRegisterReqDTO("", "not-an-email", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Error en el campo")));
    }

    private AuthResDTO createAuthResponse() {
        return AuthResDTO.builder()
                .token("jwt-token")
                .user(AuthResDTO.UserBasicInfo.builder()
                        .id(1L)
                        .username("john")
                        .email("john@gahramheit.com")
                        .build())
                .build();
    }
}
