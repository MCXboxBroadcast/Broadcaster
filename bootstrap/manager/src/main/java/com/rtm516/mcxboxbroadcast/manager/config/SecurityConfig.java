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
//            .csrf(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable()) // Disable CSRF - TODO Make this work with the start/stop/restart endpoints
//            .authorizeHttpRequests(authorize ->
//                authorize
//                    .requestMatchers("/health").permitAll()
//                    .anyRequest().authenticated()
//            )
            .httpBasic(Customizer.withDefaults())
            .formLogin(Customizer.withDefaults()); // TODO Replace this with a custom login page
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
