package com.danver.messengerserver.filters;

import com.danver.messengerserver.auth.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * This filter should only be applicable after authentication/login process's completed
 */
@Slf4j
@Order(1)
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final Environment env;

    @Autowired
    public JwtTokenFilter(JwtUtil jwtUtil, Environment env) {
        this.jwtUtil = jwtUtil;
        this.env = env;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // TODO: maybe add JWT-token here?
        /*
            Endpoints to be omitted:
            - /register;
            - /login
            - some open paths ()
        */
        log.info("JwtTokenFilter:shouldNotFilter(). RequestPath: " + request.getRequestURI());
        log.info("JwtTokenFilter:shouldNotFilter(). getPathInfo: " + request.getPathInfo());
        log.info("JwtTokenFilter:shouldNotFilter(). getServletPath: " + request.getServletPath());
        if (!Boolean.parseBoolean(env.getProperty("application.authentication.enabled"))) {
            return true;
        }
        return (request.getRequestURI().matches("(.*/login.*)|(.*/authenticate.*)|(.*/auth/.*)") ||
                request.getRequestURI().matches("(.*/register.*)"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("JwtTokenFilter:doFilterInternal(). RequestPath: " + request.getRequestURI());
        log.info("JwtTokenFilter:shouldNotFilter(). getPathInfo: " + request.getPathInfo());
        log.info("JwtTokenFilter:shouldNotFilter(). getServletPath: " + request.getServletPath());
        String token = jwtUtil.resolveToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=" + "Token is NULL");
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        // We can fully set user info here instead of controllers
        // TODO: fill auth info only with JWT-token
        filterChain.doFilter(request, response);
    }
}
