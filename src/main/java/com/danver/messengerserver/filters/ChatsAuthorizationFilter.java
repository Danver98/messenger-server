package com.danver.messengerserver.filters;

import com.danver.messengerserver.models.User;
import com.danver.messengerserver.services.permission.PermissionService;
import com.danver.messengerserver.services.permission.ResourceType;
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
import com.danver.messengerserver.services.permission.PermissionType;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

/*        if (!isAuthorized(cachedRequestWrapper)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }*/
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

    private boolean isAuthorized(HttpServletRequest request) throws IOException {
        byte[] data = StreamUtils.copyToByteArray(request.getInputStream());
        Map<String, Object> requestBody = new ObjectMapper().readValue(data, Map.class);
        String methodName = request.getMethod();
        String path = request.getServletPath();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long chatId = (Long) requestBody.get("chatId");
        if (chatId == null) {
            String requestResourceObject = request.getHeader("X-request-resource-object");
            if (requestResourceObject != null && !requestResourceObject.isEmpty()) {
                chatId = Long.parseLong(requestResourceObject);
            }
        }
        String permission = PermissionType.Chat.DEFAULT.getValue();
        int resourceType = ResourceType.CHAT.getValue();
        if (methodName.equals("POST") && path.equals("/chats/")) {
            /*
                LIST chats
                Checked directly in controller
             */
            return true;
        }
        // Default check: user can complete operations if he's present in the chat
        return permissionService.isAuthorized((User) authentication.getPrincipal(),
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
