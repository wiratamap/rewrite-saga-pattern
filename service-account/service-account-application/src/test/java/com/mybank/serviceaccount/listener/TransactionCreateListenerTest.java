package com.mybank.serviceaccount.listener;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.serviceaccount.model.Account;
import com.mybank.serviceaccount.repository.IndexNameConstant;
import com.mybank.serviceaccount.testconfiguration.ElasticsearchConfiguration;
import com.mybank.servicetransaction.eventmodel.BaseEvent;
import com.mybank.servicetransaction.eventmodel.TransactionEvent;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch.core.get.GetResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ElasticsearchConfiguration.class)
class TransactionCreateListenerTest {
  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  private ElasticsearchClient elasticsearchClient;

  @Autowired
  private ObjectMapper objectMapper;

  private static final int VERY_MINIMUM_WAITING_TIME = 10;

  @AfterEach
  public void tearDown() throws IOException {
    elasticsearchClient.deleteByQuery(query -> query
      .index(IndexNameConstant.ACCOUNTS)
      .query(criteriaQuery -> criteriaQuery
        .matchAll(value -> value
          .queryName("")))
      .conflicts(Conflicts.Proceed));
  }

  @Test
  void onTransactionCreateRequest_whenSourceAndDestinationAccountIsExists_shouldUpdateBothAccountBalance() throws IOException {
    Account sourceAccount = objectMapper.readValue(
      new ClassPathResource("json/base/sourceAccount.json").getFile(),
      new TypeReference<Account>() {});
    Account destinationAccount = objectMapper.readValue(
      new ClassPathResource("json/base/destinationAccount.json").getFile(),
      new TypeReference<Account>() {});
    elasticsearchClient.index(indexFunction -> indexFunction
      .id(sourceAccount.getAccountNumber())
      .index(IndexNameConstant.ACCOUNTS)
      .document(sourceAccount));
    elasticsearchClient.index(indexFunction -> indexFunction
      .id(destinationAccount.getAccountNumber())
      .index(IndexNameConstant.ACCOUNTS)
      .document(destinationAccount));
    BaseEvent<TransactionEvent> transactionEvent = objectMapper.readValue(
      new ClassPathResource("json/event/transactionEvent.json").getFile(),
      new TypeReference<BaseEvent<TransactionEvent>>() {});
    String jsonEvent = objectMapper.writeValueAsString(transactionEvent);

    kafkaTemplate.send("SERVICE_TRANSACTION_TRANSACTION_CREATE", jsonEvent);

    await()
      .atMost(Duration.ofSeconds(VERY_MINIMUM_WAITING_TIME))
      .untilAsserted(() -> {
        Account updatedSourceAccount = Optional.ofNullable(elasticsearchClient.get(getFunction -> getFunction
            .index(IndexNameConstant.ACCOUNTS)
            .id(sourceAccount.getAccountNumber()), Account.class))
          .map(GetResult::source)
          .orElse(Account.builder().build());
        Account updatedDestinationAccount = Optional.ofNullable(elasticsearchClient.get(getFunction -> getFunction
            .index(IndexNameConstant.ACCOUNTS)
            .id(destinationAccount.getAccountNumber()), Account.class))
          .map(GetResult::source)
          .orElse(Account.builder().build());

        assertEquals(450_000, updatedSourceAccount.getBalance());
        assertEquals(150_000, updatedDestinationAccount.getBalance());
      });
  }

  @Test
  void onTransactionCreateRequest_whenSourceAccountIsNotFound_shouldNotUpdateDestinationAccountBalance() throws IOException {
    Account destinationAccount = objectMapper.readValue(
      new ClassPathResource("json/base/destinationAccount.json").getFile(),
      new TypeReference<Account>() {});
    elasticsearchClient.index(indexFunction -> indexFunction
      .id(destinationAccount.getAccountNumber())
      .index(IndexNameConstant.ACCOUNTS)
      .document(destinationAccount));
    BaseEvent<TransactionEvent> transactionEvent = objectMapper.readValue(
      new ClassPathResource("json/event/transactionEvent.json").getFile(),
      new TypeReference<BaseEvent<TransactionEvent>>() {});
    String jsonEvent = objectMapper.writeValueAsString(transactionEvent);

    kafkaTemplate.send("SERVICE_TRANSACTION_TRANSACTION_CREATE", jsonEvent);

    await()
      .atMost(Duration.ofSeconds(VERY_MINIMUM_WAITING_TIME))
      .untilAsserted(() -> {
        Account updatedDestinationAccount = Optional.ofNullable(elasticsearchClient.get(getFunction -> getFunction
            .index(IndexNameConstant.ACCOUNTS)
            .id(destinationAccount.getAccountNumber()), Account.class))
          .map(GetResult::source)
          .orElse(Account.builder().build());

        assertEquals(100_000, updatedDestinationAccount.getBalance());
      });
  }

  @Test
  void onTransactionCreateRequest_whenDestinationAccountIsNotFound_shouldNotDeductSourceAccount() throws IOException {
    Account sourceAccount = objectMapper.readValue(
      new ClassPathResource("json/base/sourceAccount.json").getFile(),
      new TypeReference<Account>() {});
    elasticsearchClient.index(indexFunction -> indexFunction
      .id(sourceAccount.getAccountNumber())
      .index(IndexNameConstant.ACCOUNTS)
      .document(sourceAccount));
    BaseEvent<TransactionEvent> transactionEvent = objectMapper.readValue(
      new ClassPathResource("json/event/transactionEvent.json").getFile(),
      new TypeReference<BaseEvent<TransactionEvent>>() {});
    String jsonEvent = objectMapper.writeValueAsString(transactionEvent);

    kafkaTemplate.send("SERVICE_TRANSACTION_TRANSACTION_CREATE", jsonEvent);

    await()
      .atMost(Duration.ofSeconds(VERY_MINIMUM_WAITING_TIME))
      .untilAsserted(() -> {
        Account updatedSourceAccount = Optional.ofNullable(elasticsearchClient.get(getFunction -> getFunction
            .index(IndexNameConstant.ACCOUNTS)
            .id(sourceAccount.getAccountNumber()), Account.class))
          .map(GetResult::source)
          .orElse(Account.builder().build());

        assertEquals(500_000, updatedSourceAccount.getBalance());
      });
  }

  @Test
  void onTransactionCreateRequest_whenSourceAccountExceedingAmount_shouldNotUpdateBothAccountBalance() throws IOException {
    Account sourceAccount = objectMapper.readValue(
      new ClassPathResource("json/base/sourceAccount.json").getFile(),
      new TypeReference<Account>() {});
    Account destinationAccount = objectMapper.readValue(
      new ClassPathResource("json/base/destinationAccount.json").getFile(),
      new TypeReference<Account>() {});
    elasticsearchClient.index(indexFunction -> indexFunction
      .id(sourceAccount.getAccountNumber())
      .index(IndexNameConstant.ACCOUNTS)
      .document(sourceAccount));
    elasticsearchClient.index(indexFunction -> indexFunction
      .id(destinationAccount.getAccountNumber())
      .index(IndexNameConstant.ACCOUNTS)
      .document(destinationAccount));
    BaseEvent<TransactionEvent> transactionEvent = objectMapper.readValue(
      new ClassPathResource("json/event/transactionEvent.json").getFile(),
      new TypeReference<BaseEvent<TransactionEvent>>() {});
    String jsonEvent = objectMapper.writeValueAsString(transactionEvent);
    transactionEvent.getPayload().setAmount(15_000_000);

    kafkaTemplate.send("SERVICE_TRANSACTION_TRANSACTION_CREATE", jsonEvent);

    await()
      .atMost(Duration.ofSeconds(VERY_MINIMUM_WAITING_TIME))
      .untilAsserted(() -> {
        Account updatedSourceAccount = Optional.ofNullable(elasticsearchClient.get(getFunction -> getFunction
            .index(IndexNameConstant.ACCOUNTS)
            .id(sourceAccount.getAccountNumber()), Account.class))
          .map(GetResult::source)
          .orElse(Account.builder().build());
        Account updatedDestinationAccount = Optional.ofNullable(elasticsearchClient.get(getFunction -> getFunction
            .index(IndexNameConstant.ACCOUNTS)
            .id(destinationAccount.getAccountNumber()), Account.class))
          .map(GetResult::source)
          .orElse(Account.builder().build());

        assertEquals(500_000, updatedSourceAccount.getBalance());
        assertEquals(100_000, updatedDestinationAccount.getBalance());
      });
  }
}
