package com.arenacore.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Public routes - no token required
        if(path.startsWith("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);           //  need to remove: "Bearer "

        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)                      // throws exception if invalid or expired
                    .getPayload();

            String playerId = String.valueOf(claims.get("playerId"));
            String username = claims.getSubject();

            HttpServletRequest wrappedRequest = new HeaderInjectingRequestWrapper(request, playerId, username);
            filterChain.doFilter(wrappedRequest, response);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired token");
        }
    }
    /**
     * Wraps the original request to inject extra headers (X-Player-Id, X-Player-Username)
     * without modifying the original request object directly.
     */
    private static class HeaderInjectingRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String> customHeaders = new HashMap<>();

        public HeaderInjectingRequestWrapper(HttpServletRequest request, String playerId, String username) {
            super(request);
            customHeaders.put("X-Player-Id", playerId);
            customHeaders.put("X-Player-Username", username);
        }

        @Override
        public String getHeader(String name) {
            if ("Authorization".equalsIgnoreCase(name)) return null;                 // hide the original JWT from downstream

            String customValue = customHeaders.get(name);
            if (customValue != null) {
                return customValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("Authorization".equalsIgnoreCase(name))  return Collections.emptyEnumeration();

            String customValue = customHeaders.get(name);
            if (customValue != null) {
                return Collections.enumeration(Collections.singletonList(customValue));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            java.util.Set<String> names = new java.util.HashSet<>(customHeaders.keySet());
            Enumeration<String> originalNames = super.getHeaderNames();
            while (originalNames.hasMoreElements()) {
                String n = originalNames.nextElement();
                if (!"Authorization".equalsIgnoreCase(n)) {
                    names.add(n);
                }
            }
            return Collections.enumeration(names);
        }
    }
}


