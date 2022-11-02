package com.mybank.servicetransaction.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.mybank.servicetransaction.dto.request.TransactionRequest;
import com.mybank.servicetransaction.dto.response.TransactionResponse;
import com.mybank.servicetransaction.enumeration.TransactionStatus;
import com.mybank.servicetransaction.enumeration.TransactionType;
import com.mybank.servicetransaction.eventmodel.TransactionEvent;
import com.mybank.servicetransaction.model.Transaction;
import com.mybank.servicetransaction.outboundevent.PublisherService;
import com.mybank.servicetransaction.properties.EventProperties;
import com.mybank.servicetransaction.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TransactionService {

  private final TransactionRepository transactionRepository;

  private final PublisherService publisherService;

  private final EventProperties eventProperties;

  private static final String MY_BANK = "MyBank";

  public Mono<TransactionResponse> create(TransactionRequest transactionRequest) {
    return Mono.just(transactionRequest)
      .flatMap(this::toSourceAccountTransaction)
      .flatMap(transaction -> Mono.zip(
        this.transactionRepository.save(transaction),
        this.publisherService.publish(toTransactionEvent(transaction), eventProperties.getOutbound().getTransactions())
      ).map(tuple -> transaction))
      .map(this::toTransactionResponse);
  }

  private TransactionResponse toTransactionResponse(Transaction transaction) {
    return TransactionResponse.builder()
      .transactionId(transaction.getTransactionId())
      .amount(transaction.getAmount())
      .currency(transaction.getCurrency())
      .note(transaction.getNote())
      .status(transaction.getStatus())
      .source(TransactionResponse.TransactionDetailResponse.builder()
        .accountNumber(transaction.getSource().getAccountNumber())
        .accountProvider(transaction.getSource().getAccountProvider())
        .transactionType(transaction.getDestination().getTransactionType())
        .build())
      .destination(TransactionResponse.TransactionDetailResponse.builder()
        .accountNumber(transaction.getDestination().getAccountNumber())
        .accountHolderName(transaction.getDestination().getAccountHolderName())
        .accountProvider(transaction.getDestination().getAccountProvider())
        .transactionType(transaction.getDestination().getTransactionType())
        .build())
      .timestamp(transaction.getTimestamp())
      .build();
  }

  private TransactionEvent toTransactionEvent(Transaction transaction) {
    return TransactionEvent.builder()
      .transactionId(transaction.getTransactionId())
      .amount(transaction.getAmount())
      .currency(transaction.getCurrency())
      .note(transaction.getNote())
      .status(transaction.getStatus())
      .source(TransactionEvent.DestinationDetail.builder()
        .accountNumber(transaction.getSource().getAccountNumber())
        .accountHolderName(transaction.getSource().getAccountHolderName())
        .accountProvider(transaction.getSource().getAccountProvider())
        .transactionType(transaction.getSource().getTransactionType())
        .build())
      .destination(TransactionEvent.DestinationDetail.builder()
        .accountNumber(transaction.getDestination().getAccountNumber())
        .accountHolderName(transaction.getDestination().getAccountHolderName())
        .accountProvider(transaction.getDestination().getAccountProvider())
        .transactionType(transaction.getDestination().getTransactionType())
        .build())
      .timestamp(transaction.getTimestamp())
      .build();
  }

  private Mono<Transaction> toSourceAccountTransaction(TransactionRequest request) {
    return Mono.just(Transaction.builder()
      .transactionId(UUID.randomUUID())
      .amount(request.getAmount())
      .currency(request.getCurrency())
      .note(request.getNote())
      .status(TransactionStatus.PENDING)
      .source(Transaction.DestinationDetail.builder()
        .accountNumber(request.getSourceAccountNumber())
        .accountProvider(MY_BANK)
        .transactionType(TransactionType.DEBIT)
        .build())
      .destination(Transaction.DestinationDetail.builder()
        .accountNumber(request.getDestinationAccountRequest().getAccountNumber())
        .accountHolderName(request.getDestinationAccountRequest().getAccountHolderName())
        .accountProvider(request.getDestinationAccountRequest().getAccountProvider())
        .transactionType(TransactionType.CREDIT)
        .build())
      .timestamp(System.currentTimeMillis())
      .build());
  }

  public Mono<Transaction> update(Transaction transaction) {
    return this.transactionRepository.update(transaction);
  }
}
