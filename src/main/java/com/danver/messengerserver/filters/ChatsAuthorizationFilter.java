package com.danver.messengerserver.filters;

import com.danver.messengerserver.services.permission.PermissionService;
import com.danver.messengerserver.services.permission.ResourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.danver.messengerserver.services.permission.PermissionType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.danver.messengerserver.utils.Constants.X_REQUEST_RESOURCE_OBJECT;

@Slf4j
@Component
public class ChatsAuthorizationFilter extends OncePerRequestFilter {

    private final PermissionService permissionService;
    @Value("${application.authorization.enabled}")
    private boolean appAuthorizationEnabled;

    @Autowired
    public ChatsAuthorizationFilter(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CachedRequestWrapper cachedRequestWrapper =
                new CachedRequestWrapper(request);

        if (!isAuthorized(cachedRequestWrapper)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        filterChain.doFilter(cachedRequestWrapper, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        if (!appAuthorizationEnabled) return true;
        log.info("JwtTokenFilter:doFilterInternal(). RequestPath: " + request.getRequestURI());
        log.info("JwtTokenFilter:shouldNotFilter(). getPathInfo: " + request.getPathInfo());
        log.info("JwtTokenFilter:shouldNotFilter(). getServletPath: " + request.getServletPath());
        log.info("JwtTokenFilter:shouldNotFilter(). request.getContextPath: " + request.getContextPath());
        String path = request.getServletPath();
        return !path.matches(".*/chats/.*");
    }

    private boolean isAuthorized(HttpServletRequest request) throws IOException {
        byte[] data = StreamUtils.copyToByteArray(request.getInputStream());
        Map<String, Object> requestBody = new HashMap<>();
        try {
            requestBody = new ObjectMapper().readValue(data, Map.class);
        } catch (IOException ignored) {

        }
        String methodName = request.getMethod();
        String path = request.getServletPath();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long chatId = null;
        if (requestBody.get("chatId") != null){
            chatId = Long.parseLong(String.valueOf(requestBody.get("chatId")));
        }
        if (chatId == null) {
            String requestResourceObject = request.getHeader(X_REQUEST_RESOURCE_OBJECT);
            if (requestResourceObject != null && !requestResourceObject.isEmpty()) {
                try {
                    chatId = Long.parseLong(requestResourceObject);
                } catch ( NumberFormatException ignored) {

                }
            }
        }
        String permission = PermissionType.Chat.DEFAULT.getValue();
        int resourceType = ResourceType.CHAT.getValue();
        if (methodName.equals("POST") && (path.equals("/chats/") || path.equals("/chats/light-list"))) {
            /*
                LIST chats
                Checked directly in controller
             */
            return true;
        } else if (path.matches("^/chats/(\\d+).*")) {
            // TODO: replace resource objectId extraction with headers
            Pattern pattern = Pattern.compile("^/chats/(\\d+).*");
            Matcher matcher = pattern.matcher(path);
            chatId = Long.parseLong(path.split("/")[2]);
        }
        // Default check: user can complete operations if he's present in the chat
        return permissionService.isAuthorized( (UserDetails) authentication.getPrincipal(),
                chatId, resourceType,
                permission);
        /*else if (methodName.equals("GET") && path.matches("^/chats/(\\d+)$")) {
            *//*
                READ
             *//*
            permission = PermissionType.Chat.READ.getValue();
            Pattern pattern = Pattern.compile("^/chats/(\\d+)$");
            Matcher matcher = pattern.matcher(path);
            chatId = Long.parseLong(matcher.group(1));
        } else if (methodName.equals("POST") && path.equals("/chats/create")) {
            *//*
                CREATE
             *//*
            permission = PermissionType.Chat.CREATE.getValue();
            chatId = null;
        } else if (methodName.equals("PUT") && path.matches("^/chats/(\\d+)$")) {
            *//*
                EDIT
             *//*
            permission = PermissionType.Chat.EDIT.getValue();
            Pattern pattern = Pattern.compile("^/chats/(\\d+)$");
            Matcher matcher = pattern.matcher(path);
            chatId = Long.parseLong(matcher.group(1));
        } else if (methodName.equals("DELETE") && path.matches("^/chats/(\\d+)$")) {
            *//*
                DELETE
             *//*
            permission = PermissionType.Chat.DELETE.getValue();
            Pattern pattern = Pattern.compile("^/chats/(\\d+)$");
            Matcher matcher = pattern.matcher(path);
            chatId = Long.parseLong(matcher.group(1));
        } else if (methodName.equals("GET") && path.matches("^/chats/(\\d+)/participants$")) {
            *//*
                LIST PARTICIPANTS (the same as READ chat)
             *//*
            permission = PermissionType.Chat.READ.getValue();
            Pattern pattern = Pattern.compile("^/chats/(\\d+)$");
            Matcher matcher = pattern.matcher(path);
            chatId = Long.parseLong(matcher.group(1));
        } else if (methodName.equals("POST") && path.matches("^/chats/add$")) {
            *//*
                ADD participant
             *//*
            permission = PermissionType.Chat.SEND.getValue();
        } else if (methodName.equals("POST") && path.matches("^/chats/attachment$")) {
            *//*
                Send attachment
             *//*
            permission = PermissionType.Chat.SEND.getValue();
            chatId = Long.parseLong(request.getParameter("chatId"));
        } else {
            *//*
                Check whether user is a participant of the chat
             *//*
            permission = PermissionType.Chat.DEFAULT.getValue();
            // TODO: how to get chatId?
        }*/
    }
}
