// src/main/java/com/ronak/welcome/config/RateLimitInterceptor.java
package com.ronak.welcome.config;

import com.ronak.welcome.service.impl.RateLimiterService; // Import your RateLimiterService
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Get client identifier (e.g., IP address)
        // In a real production environment, if you are behind a load balancer or proxy,
        // you would typically use the "X-Forwarded-For" header to get the real client IP.
        // For local development, getRemoteAddr() is usually sufficient.
        String clientId = request.getRemoteAddr();

        // If you want to rate limit by authenticated user ID instead of IP, you would use:
        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // String clientId = (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName()))
        //                   ? authentication.getName() : request.getRemoteAddr();

        if (rateLimiterService.tryConsumeToken(clientId)) {
            return true; // Request allowed to proceed
        } else {
            // Request rate-limited: set 429 status and send a message
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests. Please try again later.");
            return false; // Request blocked
        }
    }
}
