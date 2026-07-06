package com.codingshuttle.youtube.hospitalManagement.security;

import com.codingshuttle.youtube.hospitalManagement.entity.type.RoleType;
import com.codingshuttle.youtube.hospitalManagement.entity.type.RoleType.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

import static com.codingshuttle.youtube.hospitalManagement.entity.type.PermissionType.APPOINTMENT_DELETE;
import static com.codingshuttle.youtube.hospitalManagement.entity.type.PermissionType.USER_MANAGE;
import static com.codingshuttle.youtube.hospitalManagement.entity.type.RoleType.ADMIN;

@Configuration
@RequiredArgsConstructor
@Slf4j
@EnableMethodSecurity
public class WebSecurityConfig {

private final JwtAuthFilter jwtAuthFilter;
private final Oauth2SucessHandler oauth2SucessHandler;
private final HandlerExceptionResolver handlerExceptionResolver;
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

      httpSecurity
              .csrf(AbstractHttpConfigurer::disable)
              .sessionManagement(sessionConfig ->
                      sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
              .authorizeHttpRequests(auth -> auth
                      .requestMatchers("/public/**","/auth/**").permitAll()
                      .requestMatchers("/admin/**").hasRole(ADMIN.name())
                      .requestMatchers(HttpMethod.DELETE,"/admin/**")
                      .hasAnyAuthority(APPOINTMENT_DELETE.name(),USER_MANAGE.name())
                      .requestMatchers("/doctors/**").hasAnyRole(RoleType.DOCTOR.name(),ADMIN.name())

                      .anyRequest().authenticated()
              )
              .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
              .oauth2Login(oAuth2->oAuth2.failureHandler(
                      (request, response, exception) -> {
                          log.error("OAuth2 error:{}", exception.getMessage());
                      })
                      .successHandler(oauth2SucessHandler)
              ).
              exceptionHandling(httpSecurityExceptionHandlingConfigurer ->
                      httpSecurityExceptionHandlingConfigurer.accessDeniedHandler
                              ((request, response, accessDeniedException) ->
                                      handlerExceptionResolver.resolveException(request,response,null,accessDeniedException)
                              ));
     //         .formLogin(Customizer.withDefaults());

      return httpSecurity.build();
  }


}
