package com.phoenix.bookingservice.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import com.phoenix.bookingservice.logging.RequestContext;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate template = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
        ClientHttpRequestInterceptor tracingHeaders = (request, body, execution) -> {
            if (RequestContext.getRequestId() != null) {
                request.getHeaders().set("X-Request-Id", RequestContext.getRequestId());
            }
            if (RequestContext.getTraceId() != null) {
                request.getHeaders().set("X-Trace-Id", RequestContext.getTraceId());
            }
            if (request.getHeaders().getFirst("traceparent") == null && RequestContext.getTraceId() != null) {
                request.getHeaders().set("traceparent", "00-" + RequestContext.getTraceId().replace("-", "") + "-0000000000000001-01");
            }
            return execution.execute(request, body);
        };
        template.setInterceptors(List.of(tracingHeaders));
        return template;
    }
}