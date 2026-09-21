package com.cloudpilot.backend.config;

import com.cloudpilot.backend.auth.JwtAuthenticationFilter;
import com.cloudpilot.backend.exception.CustomAuthenticationEntryPoint;
import com.cloudpilot.backend.exception.CustomAccessDeniedHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final CustomAuthenticationEntryPoint authenticationEntryPoint;
        private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                            CustomAuthenticationEntryPoint authenticationEntryPoint,
                            CustomAccessDeniedHandler accessDeniedHandler) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.authenticationEntryPoint = authenticationEntryPoint;
                this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        "/api/health",
                                        "/api/auth/register",
                                        "/api/auth/login",
                                        "/api/invitations/**",
                                        "/actuator/health",
                                        "/actuator/metrics",
                                        "/actuator/metrics/**",
                                        "/actuator/prometheus"
                                )
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                )

                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                authenticationEntryPoint
                        ).accessDeniedHandler(
                                accessDeniedHandler
                        )
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "http://localhost:30000"
                )
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}