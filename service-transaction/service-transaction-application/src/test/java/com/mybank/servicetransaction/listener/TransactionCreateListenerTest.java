package com.mybank.servicetransaction.listener;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import co.elastic.clients.elasticsearch._types.Conflicts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.servicetransaction.enumeration.TransactionStatus;
import com.mybank.servicetransaction.enumeration.TransactionType;
import com.mybank.servicetransaction.eventmodel.BaseEvent;
import com.mybank.servicetransaction.eventmodel.EventTransferAble;
import com.mybank.servicetransaction.helper.TransactionHelper;
import com.mybank.servicetransaction.model.Transaction;
import com.mybank.servicetransaction.testconfiguration.ElasticsearchConfiguration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ElasticsearchConfiguration.class)
class TransactionCreateListenerTest {

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  private ElasticsearchClient elasticsearchClient;

  @Autowired
  private ObjectMapper objectMapper;

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
  void onTransactionCreateSuccess_whenTransactionSuccessAndTransactionIdIsFound_shouldUpdateExistingTransactionToSuccess()
      throws IOException {
    Transaction pendingTransaction = Transaction.builder()
      .transactionId(UUID.randomUUID())
      .amount(100_000_000)
      .currency("IDR")
      .note("Lunch")
      .status(TransactionStatus.PENDING)
      .source(Transaction.AccountDetail.builder()
        .accountNumber("11111")
        .accountProvider("MyBank")
        .transactionType(TransactionType.DEBIT)
        .build())
      .destination(Transaction.AccountDetail.builder()
        .accountNumber("22222")
        .accountHolderName("Bertha Doe")
        .accountProvider("MyBank")
        .transactionType(TransactionType.CREDIT)
        .build())
      .timestamp(System.currentTimeMillis())
      .build();
    elasticsearchClient.index(index -> index
      .index("transactions")
      .id(pendingTransaction.getTransactionId().toString())
      .document(pendingTransaction));
    pendingTransaction.getSource().setAccountHolderName("John Doe");
    pendingTransaction.setStatus(TransactionStatus.SUCCESS);
    BaseEvent<EventTransferAble> successEvent = BaseEvent.builder()
      .eventId(UUID.randomUUID())
      .timestamp(System.currentTimeMillis())
      .payload(TransactionHelper.toTransactionEvent(pendingTransaction))
      .build();
    String jsonEvent = objectMapper.writeValueAsString(successEvent);

    kafkaTemplate.send("SERVICE_ACCOUNT_TRANSACTION_CREATE_SUCCESS", jsonEvent);

    await()
      .atMost(Duration.ofSeconds(2))
      .untilAsserted(() -> {
        Transaction updatedTransaction = Optional.ofNullable(elasticsearchClient.get(getAction -> getAction
              .index("transactions")
              .id(pendingTransaction.getTransactionId().toString()),
            Transaction.class))
          .map(GetResponse::source)
          .orElse(Transaction.builder().build());

        assertEquals(TransactionStatus.SUCCESS, updatedTransaction.getStatus());
      });
  }

  @Test
  void onTransactionCreateFailed_whenTransactionFailedAndTransactionIdIsFound_shouldUpdateExistingTransactionToFailed()
    throws IOException {
    Transaction pendingTransaction = Transaction.builder()
      .transactionId(UUID.randomUUID())
      .amount(100_000_000)
      .currency("IDR")
      .note("Lunch")
      .status(TransactionStatus.PENDING)
      .source(Transaction.AccountDetail.builder()
        .accountNumber("11111")
        .accountProvider("MyBank")
        .transactionType(TransactionType.DEBIT)
        .build())
      .destination(Transaction.AccountDetail.builder()
        .accountNumber("22222")
        .accountHolderName("Bertha Doe")
        .accountProvider("MyBank")
        .transactionType(TransactionType.CREDIT)
        .build())
      .timestamp(System.currentTimeMillis())
      .build();
    elasticsearchClient.index(index -> index
      .index("transactions")
      .id(pendingTransaction.getTransactionId().toString())
      .document(pendingTransaction));
    pendingTransaction.getSource().setAccountHolderName("John Doe");
    pendingTransaction.setStatus(TransactionStatus.FAILED);
    pendingTransaction.setStatusDetail("Amount exceeding source account balance");
    BaseEvent<EventTransferAble> successEvent = BaseEvent.builder()
      .eventId(UUID.randomUUID())
      .timestamp(System.currentTimeMillis())
      .payload(TransactionHelper.toTransactionEvent(pendingTransaction))
      .build();
    String jsonEvent = objectMapper.writeValueAsString(successEvent);

    kafkaTemplate.send("SERVICE_ACCOUNT_TRANSACTION_CREATE_FAILED", jsonEvent);

    await()
      .atMost(Duration.ofSeconds(2))
      .untilAsserted(() -> {
        Transaction updatedTransaction = Optional.ofNullable(elasticsearchClient.get(getAction -> getAction
              .index("transactions")
              .id(pendingTransaction.getTransactionId().toString()),
            Transaction.class))
          .map(GetResponse::source)
          .orElse(Transaction.builder().build());

        assertEquals(TransactionStatus.FAILED, updatedTransaction.getStatus());
      });
  }
}
