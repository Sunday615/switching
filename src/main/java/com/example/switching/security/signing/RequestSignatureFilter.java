package com.example.switching.security.signing;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import com.example.switching.security.filter.ApiKeyAuthFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RequestSignatureFilter extends OncePerRequestFilter {

    public static final String SIGNATURE_HEADER = "X-Request-Signature";
    public static final String TIMESTAMP_HEADER = "X-Timestamp";

    private final HmacSignatureVerifier verifier;

    public RequestSignatureFilter(HmacSignatureVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !path.equals("/api/inquiries")
                && !path.equals("/api/transfers")
                && !path.startsWith("/api/iso20022/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        try {
            verifier.verify(
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString(),
                    cachedRequest.bodyAsString(),
                    request.getHeader(SIGNATURE_HEADER),
                    request.getHeader(TIMESTAMP_HEADER),
                    request.getHeader(ApiKeyAuthFilter.API_KEY_HEADER));
            filterChain.doFilter(cachedRequest, response);
        } catch (SignatureVerificationException ex) {
            writeUnauthorized(response, request.getRequestURI(), ex.getMessage());
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String path, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("""
                {"status":401,"error":"UNAUTHORIZED","errorCode":"LFP-2003",\
                "message":"%s","path":"%s"}""".formatted(escapeJson(message), escapeJson(path)));
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
