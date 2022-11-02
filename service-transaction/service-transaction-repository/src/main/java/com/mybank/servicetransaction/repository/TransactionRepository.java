package com.mybank.servicetransaction.repository;

import org.springframework.stereotype.Component;

import com.mybank.servicetransaction.model.Transaction;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionRepository {

  private final ElasticsearchAsyncClient client;

  public Mono<Transaction> save(Transaction transaction) {
    return Mono.fromFuture(() -> this.client.index(index -> index
        .index(IndexNameConstant.TRANSACTIONS)
        .id(transaction.getTransactionId().toString())
        .document(transaction)))
      .flatMap(response -> Mono.just(transaction))
      .defaultIfEmpty(Transaction.builder().build())
      .onErrorResume(throwable -> {
        log.error("Something went wrong when saving index transactions, error: ", throwable);
        return Mono.just(Transaction.builder().build());
      });
  }
}
