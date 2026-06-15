package com.lorenzovendrame.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final Optional<JwtAuthenticationFilter> jwtFilter;
    private final Optional<GatewayHeaderAuthenticationFilter> gatewayFilter;

    public SecurityConfig(Optional<JwtAuthenticationFilter> jwtFilter,
                          Optional<GatewayHeaderAuthenticationFilter> gatewayFilter) {
        this.jwtFilter = jwtFilter;
        this.gatewayFilter = gatewayFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                );

        jwtFilter.ifPresent(filter ->
                http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));

        gatewayFilter.ifPresent(filter ->
                http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));

        return http.build();
    }
}