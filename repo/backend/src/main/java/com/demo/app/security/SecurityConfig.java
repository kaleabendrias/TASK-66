package com.demo.app.security;

import com.demo.app.infrastructure.ratelimit.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/listings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tiers/**").permitAll()
                        .requestMatchers("/api/orders/**").hasAnyRole("MEMBER", "SELLER", "WAREHOUSE_STAFF", "MODERATOR", "ADMINISTRATOR")
                        .requestMatchers("/api/reservations/**").hasAnyRole("MEMBER", "SELLER", "WAREHOUSE_STAFF", "MODERATOR", "ADMINISTRATOR")
                        .requestMatchers("/api/incidents/**").hasAnyRole("MEMBER", "SELLER", "WAREHOUSE_STAFF", "MODERATOR", "ADMINISTRATOR")
                        .requestMatchers("/api/appeals/**").hasAnyRole("MEMBER", "SELLER", "WAREHOUSE_STAFF", "MODERATOR", "ADMINISTRATOR")
                        .requestMatchers("/api/members/**").hasAnyRole("MEMBER", "SELLER", "WAREHOUSE_STAFF", "MODERATOR", "ADMINISTRATOR")
                        .requestMatchers("/api/benefits/**").hasAnyRole("MEMBER", "SELLER", "WAREHOUSE_STAFF", "MODERATOR", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/api/warehouses/**").hasAnyRole("WAREHOUSE_STAFF", "ADMINISTRATOR")
                        .requestMatchers("/api/warehouses/**").hasAnyRole("WAREHOUSE_STAFF", "ADMINISTRATOR")
                        .requestMatchers("/api/inventory/**").hasAnyRole("WAREHOUSE_STAFF", "SELLER", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/api/fulfillments/**").hasAnyRole("WAREHOUSE_STAFF", "ADMINISTRATOR", "SELLER")
                        .requestMatchers("/api/fulfillments/**").hasAnyRole("WAREHOUSE_STAFF", "ADMINISTRATOR")
                        .requestMatchers("/api/risk/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/api/audit/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/api/account-deletion/**").authenticated()
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/users/**").hasRole("ADMINISTRATOR")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("https://localhost:8443", "https://localhost:*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
