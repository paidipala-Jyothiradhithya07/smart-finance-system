package com.smartfinance.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

    		String header = request.getHeader(HttpHeaders.AUTHORIZATION);

    				if (header != null && header.startsWith(BEARER_PREFIX)) {
String token = header.substring(BEARER_PREFIX.length()).trim();
try {
Claims claims = jwtService.parse(token);
UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());

UsernamePasswordAuthenticationToken authentication =
new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(authentication);
SecurityContextHolder.setContext(context);
} catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
// Never log the token itself. We stay unauthenticated, and Security answers 401.
log.debug("Rejected JWT for {}: {}", request.getRequestURI(), ex.getClass().getSimpleName());
SecurityContextHolder.clearContext();
}
    				} else if (log.isDebugEnabled()) {
log.debug("No Bearer token on {}", request.getRequestURI());
}

filterChain.doFilter(request, response);
}
}
