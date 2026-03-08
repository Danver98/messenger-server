package com.danver.messengerserver.filters;

import com.danver.messengerserver.auth.JwtUtil;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.services.interfaces.UserService;
import com.danver.messengerserver.utils.Constants;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * This filter should only be applicable after authentication/login process's completed
 */
@Slf4j
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Autowired
    public JwtTokenFilter(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("JwtTokenFilter:doFilterInternal(). RequestPath: {}", request.getRequestURI());
        log.info("JwtTokenFilter:shouldNotFilter(). getPathInfo: {}", request.getPathInfo());
        log.info("JwtTokenFilter:shouldNotFilter(). getServletPath: {}", request.getServletPath());
        log.info("AUTHORIZATION header: {}", request.getHeader(HttpHeaders.AUTHORIZATION));
        log.info("=================");
        String token = jwtUtil.resolveToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        Claims claims = jwtUtil.getClaims(token);

        // TODO: use Redis or Some in-memory storage to get full info about user
        //  (substitution for DB call UserDetailsService.loadUserByUsername()
        UserDetails userDetails = userService.loadUserByUsername((String) claims.get(Constants.USER_JWT_LOGIN_KEY));
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }
}
