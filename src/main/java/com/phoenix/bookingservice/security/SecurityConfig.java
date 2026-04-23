package com.phoenix.bookingservice.security;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final List<String> allowedOrigins;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InternalServiceAuthFilter internalServiceAuthFilter;

    public SecurityConfig(
            @Value("${app.cors.allowed-origins:https://dev.phoenix-project.online,http://localhost:3000}") List<String> allowedOrigins,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            InternalServiceAuthFilter internalServiceAuthFilter
    ) {
        this.allowedOrigins = allowedOrigins;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.internalServiceAuthFilter = internalServiceAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF disabled intentionally: stateless REST API with JWT Bearer tokens; no session cookies.
        // CSRF protects cookie-based auth; Bearer tokens are not sent automatically by browsers.
        http
                .csrf(csrf -> csrf.disable()) // NOSONAR - intentional for stateless JWT API
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api-docs/**",
                                "/actuator/health",
                            "/actuator/health/**",
                                "/actuator/info"
                        ).permitAll()

                        .requestMatchers(OPTIONS, "/**").permitAll()

                        .requestMatchers(POST, "/bookings/payment-callback")
                        .hasAuthority(BookingPermissions.INTERNAL_SERVICE)

                        .requestMatchers(POST, "/bookings/*/expire")
                        .hasAuthority(BookingPermissions.INTERNAL_SERVICE)

                        .requestMatchers(POST, "/bookings")
                        .hasAuthority(BookingPermissions.CREATE_BOOKING)

                        .requestMatchers(GET, "/bookings")
                        .hasAuthority(BookingPermissions.VIEW_ALL_BOOKINGS)

                        .requestMatchers(GET, "/bookings/customer/**")
                        .hasAuthority(BookingPermissions.VIEW_ALL_BOOKINGS)

                        .requestMatchers(GET, "/bookings/**")
                        .hasAuthority(BookingPermissions.VIEW_BOOKINGS)

                        .requestMatchers(PATCH, "/bookings/*/cancel")
                        .hasAuthority(BookingPermissions.CANCEL_BOOKING)

                        .requestMatchers(PATCH, "/bookings/*")
                        .hasAuthority(BookingPermissions.EDIT_BOOKING)

                        .requestMatchers(POST, "/bookings/*/start-payment")
                        .hasAuthority(BookingPermissions.UPDATE_BOOKING)

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("""
                                {"status":401,"error":"Unauthorized","message":"Authentication is required"}
                            """);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("""
                                {"status":403,"error":"Forbidden","message":"You do not have permission to access this resource"}
                            """);
                        })
                )
                .addFilterBefore(internalServiceAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}