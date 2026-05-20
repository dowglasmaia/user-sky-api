package com.luciodowglas.userapi.integration.project;

import java.time.Duration;

import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

/**
 * Builds the {@link WebClient} used to reach projects-api.
 *
 * <p>The exchange filter copies {@code correlationId} and {@code traceId} from
 * the MDC onto outgoing request headers, enabling cross-service log correlation.</p>
 */
@Configuration
@EnableConfigurationProperties(ProjectsApiProperties.class)
public class WebClientConfig {

    static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    static final String TRACE_ID_HEADER = "X-Trace-Id";
    static final String CORRELATION_ID_MDC = "correlationId";
    static final String TRACE_ID_MDC = "traceId";

    @Bean
    WebClient projectsApiWebClient(ProjectsApiProperties props) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.connectTimeoutMs())
                .responseTimeout(Duration.ofMillis(props.responseTimeoutMs()));

        return WebClient.builder()
                .baseUrl(props.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(correlationPropagationFilter())
                .build();
    }

    private ExchangeFilterFunction correlationPropagationFilter() {
        return (request, next) -> {
            String correlationId = MDC.get(CORRELATION_ID_MDC);
            String traceId = MDC.get(TRACE_ID_MDC);
            ClientRequest enriched = ClientRequest.from(request)
                    .headers(headers -> {
                        if (correlationId != null) headers.set(CORRELATION_ID_HEADER, correlationId);
                        if (traceId != null) headers.set(TRACE_ID_HEADER, traceId);
                    })
                    .build();
            return next.exchange(enriched);
        };
    }
}
