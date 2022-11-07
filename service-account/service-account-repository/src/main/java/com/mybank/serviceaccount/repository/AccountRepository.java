package com.mybank.serviceaccount.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.mybank.serviceaccount.model.Account;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.InlineGet;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountRepository {

  private final ElasticsearchAsyncClient client;

  public Mono<Account> findByAccountNumber(String accountNumber) {
    return Mono.fromFuture(() -> this.client.search(searchFunction -> searchFunction
        .index(IndexNameConstant.ACCOUNTS)
        .query(query -> query
          .bool(boolQuery -> boolQuery
            .filter(filterQuery -> filterQuery
              .term(termQuery -> termQuery
                .field(FieldNameConstant.ACCOUNT_NUMBER)
                .value(accountNumber))))), Account.class))
      .map(SearchResponse::hits)
      .map(HitsMetadata::hits)
      .map(Collection::stream)
      .map(Stream::findFirst)
      .map(Optional::get)
      .mapNotNull(Hit::source)
      .defaultIfEmpty(Account.builder().build())
      .onErrorResume(throwable -> {
        log.error("Something went wrong when find by accountNumber", throwable);
        return Mono.just(Account.builder().build());
      });
  }

  public Mono<Account> update(Account account) {
    return Mono.fromFuture(() -> this.client.update(updateFunction -> updateFunction
        .index(IndexNameConstant.ACCOUNTS)
        .id(account.getAccountNumber())
        .doc(account), Account.class))
      .mapNotNull(UpdateResponse::get)
      .map(InlineGet::source)
      .onErrorResume(throwable -> {
        log.error("Something went wrong when update index transactions, error: ", throwable);
        return Mono.just(Account.builder().build());
      });
  }
}
