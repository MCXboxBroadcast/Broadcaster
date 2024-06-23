package com.rtm516.mcxboxbroadcast.manager.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()); // Disable CSRF - TODO Make this work with the start/stop/restart endpoints?

        if (System.getenv("SECURITY") == null || System.getenv("SECURITY").equals("true")) {
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
    @ConditionalOnMissingBean(UserDetailsService.class)
    InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        return new InMemoryUserDetailsManager(User.builder()
                .username("admin")
                .password(passwordEncoder().encode("password"))
            .build());
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
