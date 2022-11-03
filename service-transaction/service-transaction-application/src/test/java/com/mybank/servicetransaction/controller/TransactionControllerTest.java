package com.mybank.servicetransaction.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.mybank.servicetransaction.dto.request.TransactionRequest;
import com.mybank.servicetransaction.dto.response.TransactionResponse;
import com.mybank.servicetransaction.model.Transaction;
import com.mybank.servicetransaction.testconfiguration.ElasticsearchConfiguration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch.core.GetResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ElasticsearchConfiguration.class)
@AutoConfigureWebTestClient
class TransactionControllerTest {

  @Autowired
  private WebTestClient mockClient;

  @Autowired
  private ElasticsearchClient elasticsearchClient;

  @AfterEach
  public void tearDown() throws IOException {
    elasticsearchClient.deleteByQuery(query -> query
      .index("transactions")
      .query(criteriaQuery -> criteriaQuery
        .matchAll(value -> value
          .queryName("")))
      .conflicts(Conflicts.Proceed));
  }

  @Test
  void create_whenInvoked_shouldCreateNewTransaction() throws IOException {
    TransactionRequest transactionRequest = TransactionRequest.builder()
      .sourceAccountNumber("1111")
      .amount(10_000)
      .currency("IDR")
      .note("Lunch")
      .destinationAccountRequest(TransactionRequest.DestinationAccountRequest.builder()
        .accountHolderName("Bertha Doe")
        .accountNumber("2222")
        .accountProvider("MyBank")
        .build())
      .build();

    TransactionResponse responseBody = mockClient.post()
      .uri("/transactions")
      .body(BodyInserters.fromValue(transactionRequest))
      .exchange()
      .expectStatus()
      .isCreated()
      .expectHeader()
      .contentType(MediaType.APPLICATION_JSON)
      .expectBody(new ParameterizedTypeReference<TransactionResponse>() {
      })
      .returnResult()
      .getResponseBody();
    String transactionId = Optional.ofNullable(responseBody)
      .map(TransactionResponse::getTransactionId)
      .map(UUID::toString)
      .orElse("");
    Transaction savedTransaction = Optional.ofNullable(elasticsearchClient.get(getAction -> getAction
          .index("transactions")
          .id(transactionId),
        Transaction.class))
      .map(GetResponse::source)
      .orElse(null);

    assertNotNull(savedTransaction);
  }
}
