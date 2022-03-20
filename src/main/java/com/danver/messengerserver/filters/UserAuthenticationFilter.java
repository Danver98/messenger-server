package com.danver.messengerserver.filters;

import com.danver.messengerserver.utils.AuthUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Order(1)
@Component
public class UserAuthenticationFilter extends OncePerRequestFilter {

    private final AuthUtil authUtil;

    @Autowired
    UserAuthenticationFilter(AuthUtil authUtil) {
        this.authUtil = authUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        //It's assumed that user's already logined
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null) {
            response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=" + "Not configured yet");
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        String[] authData = authHeader.split(" ");
        Jws<Claims> claims = authUtil.getParsedAndValidatedToken(authData[1]);
        if (authData.length == 2 && authData[0].equals("Bearer") && claims != null) {
            //request.setAttribute("subject", claims.getBody().getSubject());
            filterChain.doFilter(request, response);
        } else {
            response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=" + "Not configured yet");
            response.sendError(HttpStatus.UNAUTHORIZED.value());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return (request.getRequestURI().matches(".*/users$") &&
                request.getMethod().equalsIgnoreCase("POST"));
    }
}
