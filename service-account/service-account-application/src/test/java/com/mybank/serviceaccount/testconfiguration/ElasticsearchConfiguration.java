package com.mybank.serviceaccount.testconfiguration;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.RequiredArgsConstructor;

@TestConfiguration
@RequiredArgsConstructor
public class ElasticsearchConfiguration {

  private final ElasticsearchProperties properties;

  @Bean
  public ElasticsearchClient elasticsearchClient() {
    RestClient httpClient = RestClient.builder(
      HttpHost.create(properties.getUris()
        .stream()
        .findFirst()
        .orElse("localhost:9200"))
    ).build();
    ElasticsearchTransport transport = new RestClientTransport(
      httpClient, new JacksonJsonpMapper());

    return new ElasticsearchClient(transport);
  }
}
