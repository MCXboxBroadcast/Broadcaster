package com.rtm516.mcxboxbroadcast.manager.config;

import com.rtm516.mcxboxbroadcast.manager.BackendManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    @Autowired
    public SecurityFilterChain filterChain(HttpSecurity http, BackendManager backendManager) throws Exception {
        http
            .csrf(csrf -> csrf.disable()); // Disable CSRF - TODO Make this work with the start/stop/restart endpoints?

        if (backendManager.authEnabled()) {
            http
                .authorizeHttpRequests(authorize ->
                    authorize
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/assets/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form ->
                    form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            // Send { "success": true } to the client
                            response.setStatus(200);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":true}");
                        })
                        .failureHandler((request, response, exception) -> {
                            // Send { "success": false } to the client
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":false}");
                        })
                        .permitAll()
                );
        }

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
