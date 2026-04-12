package com.hotel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.common.Result;
import com.hotel.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers("/auth/login", "/error").permitAll()

                .antMatchers(HttpMethod.GET, "/rooms/browse", "/rooms/*/detail", "/rooms/available").hasAnyRole("ADMIN", "RECEPTIONIST", "CLIENT")
                .antMatchers("/profile/**", "/auth/current-user").authenticated()
                .antMatchers("/bookings/**").hasRole("CLIENT")
                .antMatchers(HttpMethod.GET, "/orders/my").authenticated()
                .antMatchers(HttpMethod.PUT, "/orders/*/renew").authenticated()
                .antMatchers(HttpMethod.GET, "/dashboard/stats").hasAnyRole("ADMIN", "RECEPTIONIST")

                .antMatchers("/users/**").hasRole("ADMIN")
                .antMatchers("/customers/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .antMatchers("/room-types/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .antMatchers("/rooms/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .antMatchers("/orders/**").hasAnyRole("ADMIN", "RECEPTIONIST")

                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, ex) -> writeResponse(response, HttpStatus.UNAUTHORIZED, Result.error(401, "未认证或token已失效")))
                .accessDeniedHandler((request, response, ex) -> writeResponse(response, HttpStatus.FORBIDDEN, Result.error(403, "无权限访问")))
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Collections.singletonList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setExposedHeaders(Collections.singletonList("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeResponse(HttpServletResponse response, HttpStatus status, Result<?> body) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
