package com.danver.messengerserver.filters;

import com.danver.messengerserver.auth.JwtUtil;
import io.jsonwebtoken.Claims;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This filter should only be applicable after authentication/login process's completed
 */
@Slf4j
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("JwtTokenFilter:doFilterInternal(). RequestPath: " + request.getRequestURI());
        log.info("JwtTokenFilter:shouldNotFilter(). getPathInfo: " + request.getPathInfo());
        log.info("JwtTokenFilter:shouldNotFilter(). getServletPath: " + request.getServletPath());
        log.info(("AUTHORIZATION header: " + request.getHeader(HttpHeaders.AUTHORIZATION)));
        log.info("=================");
        String token = jwtUtil.resolveToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        Claims claims = jwtUtil.getClaims(token);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO: use Redis or Some in-memory storage to get full info about user
        //  (substitution for DB call UserDetailsService.loadUserByUsername()
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                claims.get("login"),
                null,
                new ArrayList<>()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }
}
