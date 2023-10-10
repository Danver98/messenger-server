package com.danver.messengerserver.filters;


import com.danver.messengerserver.exceptions.AuthorizedAccessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Order(2)
@Component
public class UserAuthorizationFilter extends OncePerRequestFilter {

    private final Environment env;

    @Autowired
    public UserAuthorizationFilter(Environment env) {
        this.env = env;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!Boolean.parseBoolean(env.getProperty("application.authentication.enabled"))) {
            return true;
        }
        return (request.getRequestURI().matches(".*/users$") &&
                request.getMethod().equalsIgnoreCase("POST") ||
                request.getRequestURI().matches(".*/users/search$"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // We suppose token is present and has been validated
        String subject = (String) request.getAttribute("subject");
        if (subject != null) {
            if (request.getRequestURI().contains(subject)) {
                // subject id is in request path
                filterChain.doFilter(request, response);
                return;
            } else {
                throw new AuthorizedAccessException();
            }
        }
        String token = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
        Base64.Decoder decoder = Base64.getUrlDecoder();
        Map<String, Object> map = new ObjectMapper().readValue(new String(decoder.decode(token.split("\\.")[1])), Map.class);
        if (request.getRequestURI().contains((String) map.get("sub"))) {
            // subject id is in request path
            filterChain.doFilter(request, response);
        } else {
            throw new AuthorizedAccessException();
        }
    }
}
