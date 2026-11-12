package com.backend.eventia.config;

import com.backend.eventia.security.JwtAccessDeniedHandler;
import com.backend.eventia.security.JwtAuthenticationEntryPoint;
import com.backend.eventia.security.JwtAuthenticationFilter;
import com.backend.eventia.security.RequestLoggingFilter;
import com.backend.eventia.service.impl.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RequestLoggingFilter requestLoggingFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final Environment environment;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {

        return httpSecurity
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(http -> {
                    // Endpoints públicos
                    http.requestMatchers("/auth/login", "/auth/register").permitAll();
                    http.requestMatchers("/error").permitAll();
                    http.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    if (isDevProfileActive()) {
                        http.requestMatchers("/h2-console/**").permitAll();
                    }

                    // El catálogo de eventos es publico
                    http.requestMatchers(HttpMethod.GET, "/api/events/organizer").hasRole("ORGANIZER");
                    http.requestMatchers(HttpMethod.GET, "/api/events", "/api/events/*").permitAll();
                    http.requestMatchers(HttpMethod.GET, "/api/events/*/ticket-types").permitAll();

                    // Órdenes y tickets del usuario autenticado
                    http.requestMatchers(HttpMethod.POST, "/api/orders").authenticated();
                    http.requestMatchers(HttpMethod.GET, "/api/orders").authenticated();
                    http.requestMatchers(HttpMethod.GET, "/api/tickets/my").authenticated();
                    http.requestMatchers(HttpMethod.PATCH, "/api/tickets/*/nominate").authenticated();

                    // Escritura de eventos
                    http.requestMatchers(HttpMethod.POST, "/api/events")
                            .hasRole("ORGANIZER");

                    http.requestMatchers(HttpMethod.PUT, "/api/events/*")
                            .hasRole("ORGANIZER");

                    http.requestMatchers(HttpMethod.PATCH, "/api/events/*/status", "/api/events/*/active")
                            .hasRole("ORGANIZER");

                    // Ticket types: lectura pública para eventos publicados, escritura solo ORGANIZER
                    http.requestMatchers(HttpMethod.POST, "/api/events/*/ticket-types")
                            .hasRole("ORGANIZER");
                    http.requestMatchers(HttpMethod.PUT, "/api/ticket-types/*")
                            .hasRole("ORGANIZER");
                    http.requestMatchers(HttpMethod.PATCH, "/api/ticket-types/*/status")
                            .hasRole("ORGANIZER");
                    http.requestMatchers(HttpMethod.DELETE, "/api/ticket-types/*")
                            .hasRole("ORGANIZER");

                    // Categorias: catalogo de solo lectura
                    http.requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/*").permitAll();

                    // Paneles de control
                    http.requestMatchers("/api/organizer/**").hasRole("ORGANIZER");
                    http.anyRequest().authenticated();
                })
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(requestLoggingFilter, JwtAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .build();
    }

    private boolean isDevProfileActive() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.asList(activeProfiles).contains("dev")
                || (activeProfiles.length == 0
                && ("dev".equals(environment.getProperty("spring.profiles.default"))
                || Arrays.asList(environment.getDefaultProfiles()).contains("dev")));
    }


    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilterRegistration(RequestLoggingFilter filter) {
        // Se ejecuta dentro de la cadena de seguridad, no como filtro adicional del contenedor.
        FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }


    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsServiceImpl userDetailsService) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthenticationProvider;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
