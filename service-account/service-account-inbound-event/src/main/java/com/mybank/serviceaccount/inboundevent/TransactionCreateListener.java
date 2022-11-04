package com.mybank.serviceaccount.inboundevent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.serviceaccount.service.AccountService;
import com.mybank.servicetransaction.eventmodel.BaseEvent;
import com.mybank.servicetransaction.eventmodel.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionCreateListener {

  private final ObjectMapper objectMapper;

  private final AccountService accountService;

  @KafkaListener(topics = "${event.topic.inbound.transaction}")
  public void onTransactionCreateRequest(ConsumerRecord<String, String> consumerRecord) {
    Mono.fromSupplier(() -> consumerRecord)
      .map(ConsumerRecord::value)
      .mapNotNull(this::toEvent)
      .doOnNext(event -> log.info("Consume transaction create request from topic {}, with event {}", consumerRecord.topic(), event))
      .map(BaseEvent::getPayload)
      .flatMap(accountService::processTransaction)
      .subscribe();
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
