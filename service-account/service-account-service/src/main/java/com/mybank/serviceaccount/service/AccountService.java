package com.mybank.serviceaccount.service;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.mybank.serviceaccount.exception.AccountNotFoundException;
import com.mybank.serviceaccount.model.Account;
import com.mybank.serviceaccount.outboundevent.PublisherService;
import com.mybank.serviceaccount.properties.EventProperties;
import com.mybank.serviceaccount.repository.AccountRepository;
import com.mybank.servicetransaction.enumeration.TransactionStatus;
import com.mybank.servicetransaction.eventmodel.TransactionEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

  private final AccountRepository accountRepository;

  private final PublisherService publisherService;

  private final EventProperties eventProperties;

  private static final String EXCEEDING_AMOUNT_STATUS_DETAIL = "Amount exceeding source account balance";

  public Mono<Boolean> processTransaction(TransactionEvent transactionEvent) {
    return Mono.fromSupplier(() -> transactionEvent)
      .flatMap(incomingTransaction -> Mono.zip(
        accountRepository
          .findByAccountNumber(toSourceAccountNumber(incomingTransaction)),
        accountRepository
          .findByAccountNumber(toDestinationAccountNumber(incomingTransaction)),
          Mono.just(incomingTransaction)
      ))
      .flatMap(this::validateExistingCustomer)
      .flatMap(this::validateAccountBalance)
      .flatMap(this::updateBothAccountBalance)
      .flatMap(this::updateToElasticsearch);
  }

  private Mono<Boolean> updateToElasticsearch(Tuple3<Account, Account, TransactionEvent> tuple) {
    Account source = tuple.getT1();
    Account destination = tuple.getT2();
    TransactionEvent incomingTransaction = tuple.getT3();

    return Mono.zip(
        accountRepository.update(source),
        accountRepository.update(destination),
        publisherService.publish(toSuccessTransactionEvent(incomingTransaction, source),
          eventProperties.getOutbound().getSuccessTransaction()))
      .flatMap(zipCall -> Mono.just(Boolean.TRUE));
  }

  private String toSourceAccountNumber(TransactionEvent incomingTransaction) {
    return Optional.ofNullable(incomingTransaction)
      .map(TransactionEvent::getSource)
      .map(TransactionEvent.AccountDetail::getAccountNumber)
      .orElse("");
  }

  private String toDestinationAccountNumber(TransactionEvent incomingTransaction) {
    return Optional.ofNullable(incomingTransaction)
      .map(TransactionEvent::getDestination)
      .map(TransactionEvent.AccountDetail::getAccountNumber)
      .orElse("");
  }

  private Mono<Tuple3<Account, Account, TransactionEvent>> updateBothAccountBalance(
      Tuple3<Account, Account, TransactionEvent> tuple) {
    Account source = tuple.getT1();
    Account destination = tuple.getT2();
    TransactionEvent incomingTransaction = tuple.getT3();

    long updatedSourceBalance = source.getBalance() - incomingTransaction.getAmount();
    long updatedDestinationBalance = destination.getBalance() + incomingTransaction.getAmount();
    source.setBalance(updatedSourceBalance);
    destination.setBalance(updatedDestinationBalance);

    return Mono.zip(Mono.just(source), Mono.just(destination), Mono.just(incomingTransaction));
  }

  private Mono<Tuple3<Account, Account, TransactionEvent>> validateAccountBalance(
      Tuple3<Account, Account, TransactionEvent> tuple) {
    Account source = tuple.getT1();
    TransactionEvent incomingTransaction = tuple.getT3();

    if (incomingTransaction.getAmount() > source.getBalance()) {
      return Mono.fromSupplier(() -> EXCEEDING_AMOUNT_STATUS_DETAIL)
        .flatMap(statusDetail -> publisherService.publish(
            toFailedTransactionEvent(incomingTransaction, source, statusDetail),
            eventProperties.getOutbound().getFailedTransaction())
          .map(val -> statusDetail))
        .doOnNext(statusDetail -> log.error(statusDetail, incomingTransaction.getSource().getAccountNumber()))
        .flatMap(val -> Mono.error(AccountNotFoundException::new));
    }

    return Mono.just(tuple);
  }

  private Mono<Tuple3<Account, Account, TransactionEvent>> validateExistingCustomer(
      Tuple3<Account, Account, TransactionEvent> tuple) {
    Account source = tuple.getT1();
    Account destination = tuple.getT2();
    TransactionEvent incomingTransaction = tuple.getT3();

    if (Objects.isNull(source.getAccountNumber())) {
      return Mono.fromSupplier(() -> toNotFoundAccountStatusDetail(incomingTransaction.getSource().getAccountNumber()))
        .flatMap(statusDetail -> publisherService.publish(
            toFailedTransactionEvent(incomingTransaction, source, statusDetail),
            eventProperties.getOutbound().getFailedTransaction())
          .map(val -> statusDetail))
        .doOnNext(statusDetail -> log.error(statusDetail, incomingTransaction.getSource().getAccountNumber()))
        .flatMap(val -> Mono.error(AccountNotFoundException::new));
    }
    if (Objects.isNull(destination.getAccountNumber())) {
      return Mono.fromSupplier(() -> toNotFoundAccountStatusDetail(incomingTransaction.getDestination().getAccountNumber()))
        .flatMap(statusDetail -> publisherService.publish(
            toFailedTransactionEvent(incomingTransaction, source, statusDetail),
            eventProperties.getOutbound().getFailedTransaction())
          .map(val -> statusDetail))
        .doOnNext(statusDetail -> log.error(statusDetail, incomingTransaction.getDestination().getAccountNumber()))
        .flatMap(val -> Mono.error(AccountNotFoundException::new));
    }

    return Mono.just(tuple);
  }

  private String toNotFoundAccountStatusDetail(String accountNumber) {
    return String.format("Account with customer number %s not found", accountNumber);
  }

  private TransactionEvent toSuccessTransactionEvent(TransactionEvent transactionEvent,
      Account source) {
    transactionEvent.setStatus(TransactionStatus.SUCCESS);
    transactionEvent.getSource().setAccountNumber(source.getAccountNumber());
    transactionEvent.getSource().setAccountHolderName(source.getCardHolderName());

    return transactionEvent;
  }

  private TransactionEvent toFailedTransactionEvent(TransactionEvent transactionEvent,
      Account source, String statusDetail) {
    transactionEvent.setStatus(TransactionStatus.FAILED);
    transactionEvent.setStatusDetail(statusDetail);
    transactionEvent.getSource().setAccountNumber(source.getAccountNumber());
    transactionEvent.getSource().setAccountHolderName(source.getCardHolderName());

    return transactionEvent;
  }
}
