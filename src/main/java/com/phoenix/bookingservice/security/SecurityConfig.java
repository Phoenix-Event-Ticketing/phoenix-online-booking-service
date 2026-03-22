package com.phoenix.bookingservice.security;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InternalServiceAuthFilter internalServiceAuthFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            InternalServiceAuthFilter internalServiceAuthFilter
    ) {
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
                                "/actuator/info"
                        ).permitAll()

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
}