package com.mybank.servicetransaction.inboundevent;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.servicetransaction.eventmodel.BaseEvent;
import com.mybank.servicetransaction.eventmodel.TransactionEvent;
import com.mybank.servicetransaction.model.Transaction;
import com.mybank.servicetransaction.service.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionCreateListener {

  private final TransactionService transactionService;

  private final ObjectMapper objectMapper;

  @KafkaListener(topics = "${event.topic.inbound.success-transaction}")
  void onTransactionCreateSuccess(ConsumerRecord<String, String> consumerRecord) {
    Mono.fromSupplier(() -> consumerRecord)
      .map(ConsumerRecord::value)
      .mapNotNull(this::toEvent)
      .doOnNext(event -> log.info("Consume success event from topic {}, with event {}", consumerRecord.topic(), event))
      .map(BaseEvent::getPayload)
      .map(this::toPartialTransaction)
      .flatMap(transactionService::update)
      .subscribe();
  }

  @KafkaListener(topics = "${event.topic.inbound.failed-transaction}")
  void onTransactionCreateFailed(ConsumerRecord<String, String> consumerRecord) {
    Mono.fromSupplier(() -> consumerRecord)
      .map(ConsumerRecord::value)
      .mapNotNull(this::toEvent)
      .doOnNext(event -> log.info("Consume failed event from topic {}, with event {}", consumerRecord.topic(), event))
      .map(BaseEvent::getPayload)
      .map(this::toPartialTransaction)
      .flatMap(transactionService::update)
      .subscribe();
  }

  private Transaction toPartialTransaction(TransactionEvent event) {
    return Transaction.builder()
      .transactionId(event.getTransactionId())
      .amount(event.getAmount())
      .currency(event.getCurrency())
      .note(event.getNote())
      .status(event.getStatus())
      .statusDetail(event.getStatusDetail())
      .source(Transaction.DestinationDetail.builder()
        .accountNumber(event.getSource().getAccountNumber())
        .accountHolderName(event.getSource().getAccountHolderName())
        .accountProvider(event.getSource().getAccountProvider())
        .transactionType(event.getSource().getTransactionType())
        .build())
      .destination(Transaction.DestinationDetail.builder()
        .accountNumber(event.getDestination().getAccountNumber())
        .accountHolderName(event.getDestination().getAccountHolderName())
        .accountProvider(event.getDestination().getAccountProvider())
        .transactionType(event.getDestination().getTransactionType())
        .build())
      .timestamp(event.getTimestamp())
      .build();
  }

  private BaseEvent<TransactionEvent> toEvent(String message) {
    try {
      return objectMapper.readValue(message, new TypeReference<BaseEvent<TransactionEvent>>() {});
    } catch (Exception e) {
      log.error("Error when parsing json to event model", e);
      return null;
    }
  }
}
