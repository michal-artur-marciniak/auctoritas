package dev.auctoritas.auth.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Bean
  @ConditionalOnMissingBean(RestClient.Builder.class)
  RestClient.Builder restClientBuilder() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
    requestFactory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());

    return RestClient.builder().requestFactory(requestFactory);
  }
}
