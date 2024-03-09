package com.danver.messengerserver.filters;

import com.danver.messengerserver.services.permission.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@Slf4j
@Component
public class ChatsAuthorizationFilter extends OncePerRequestFilter {

    private final PermissionService permissionService;

    @Autowired
    public ChatsAuthorizationFilter(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CachedRequestWrapper cachedRequestWrapper =
                new CachedRequestWrapper(request);

        byte [] data = StreamUtils.copyToByteArray(cachedRequestWrapper.getInputStream());
        Map<String, Object> requestBody = new ObjectMapper().readValue(data, Map.class);
        String methodName = request.getMethod();
        String path = request.getServletPath();
        Principal principal = request.getUserPrincipal();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        filterChain.doFilter(cachedRequestWrapper, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        log.info("JwtTokenFilter:doFilterInternal(). RequestPath: " + request.getRequestURI());
        log.info("JwtTokenFilter:shouldNotFilter(). getPathInfo: " + request.getPathInfo());
        log.info("JwtTokenFilter:shouldNotFilter(). getServletPath: " + request.getServletPath());
        log.info("JwtTokenFilter:shouldNotFilter(). request.getContextPath: " + request.getContextPath());
        String path = request.getServletPath();
        return !path.matches(".*/chats/.*");
    }
}
