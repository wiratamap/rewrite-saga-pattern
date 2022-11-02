package com.mybank.servicetransaction.repository;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.InlineGet;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import com.mybank.servicetransaction.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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

  public Mono<Transaction> update(Transaction transaction) {
    return Mono.fromFuture(() -> this.client.update(index -> index
        .index(IndexNameConstant.TRANSACTIONS)
        .id(transaction.getTransactionId().toString())
        .doc(transaction),
        Transaction.class))
      .mapNotNull(UpdateResponse::get)
      .map(InlineGet::source)
      .onErrorResume(throwable -> {
        log.error("Something went wrong when update index transactions, error: ", throwable);
        return Mono.just(Transaction.builder().build());
      });
  }
}
