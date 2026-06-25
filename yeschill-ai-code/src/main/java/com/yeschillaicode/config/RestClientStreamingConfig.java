package com.yeschillaicode.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 配置 RestClient 使用 JdkClientHttpRequestFactory 以支持真正的 SSE 流式传输。
 *
 * Spring Boot 默认使用 SimpleClientHttpRequestFactory（基于 HttpURLConnection），
 * 它会缓冲整个 HTTP 响应体后才返回，导致 SSE 流式输出变成一次性返回。
 * JdkClientHttpRequestFactory 基于 Java 11+ HttpClient，支持真正的增量流式读取。
 */
@Configuration
public class RestClientStreamingConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(180));

        return RestClient.builder().requestFactory(requestFactory);
    }
}
