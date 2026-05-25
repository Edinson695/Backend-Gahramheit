package com.example.gahramheit.controller;

import com.example.gahramheit.security.JwtAuthenticationEntryPoint;
import com.example.gahramheit.security.JwtAuthenticationFilter;
import com.example.gahramheit.security.UserDetailsServiceImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

abstract class AbstractControllerTest {

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;
}
