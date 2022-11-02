package com.mybank.servicetransaction.outboundevent;

import java.util.List;
import java.util.UUID;

import com.mybank.servicetransaction.eventmodel.BaseEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.servicetransaction.eventmodel.EventTransferAble;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class PublisherService {
  private final KafkaTemplate<String, String> kafkaTemplate;

  private final ObjectMapper objectMapper;

  public <T extends EventTransferAble> Mono<Boolean> publish(T event, List<String> topics) {
    return Flux.fromIterable(topics)
      .flatMap(topic -> Mono.fromFuture(kafkaTemplate.send(topic, toJson(event)).completable())
          .map(result -> Boolean.TRUE))
      .collectList()
      .flatMap(result -> Mono.just(Boolean.TRUE));
  }

  private <T extends EventTransferAble> String toJson(T event) {
    try {
      BaseEvent<EventTransferAble> baseEvent = BaseEvent.builder()
        .eventId(UUID.randomUUID())
        .timestamp(System.currentTimeMillis())
        .payload(event)
        .build();
      return this.objectMapper.writeValueAsString(baseEvent);
    } catch (JsonProcessingException exception) {
      log.error("Something went wrong when publish event ", exception);
      return "";
    }
  }
}
