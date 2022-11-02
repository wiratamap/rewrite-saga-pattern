package com.mybank.servicetransaction.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ElasticsearchConfiguration {
  private final ElasticsearchProperties properties;

  @Bean
  public ElasticsearchAsyncClient client() {
    RestClient httpClient = RestClient.builder(
      HttpHost.create(properties.getUris()
        .stream()
        .findFirst()
        .orElse("localhost:9200"))
    ).build();
    ElasticsearchTransport transport = new RestClientTransport(
      httpClient, new JacksonJsonpMapper());

    return new ElasticsearchAsyncClient(transport);
  }
}
