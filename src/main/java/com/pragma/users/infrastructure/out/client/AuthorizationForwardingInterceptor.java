package com.pragma.users.infrastructure.out.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

/**
 * Propagates the inbound {@code Authorization} header of the current HTTP request to any
 * outbound RestClient call (food-court). This allows the owner JWT to be forwarded so
 * food-court can re-validate restaurant ownership (defense-in-depth).
 */
public class AuthorizationForwardingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null) {
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
        return execution.execute(request, body);
    }
}
