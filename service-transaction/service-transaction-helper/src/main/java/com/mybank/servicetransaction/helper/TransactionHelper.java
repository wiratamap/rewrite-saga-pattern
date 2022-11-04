package com.mybank.servicetransaction.helper;

import com.mybank.servicetransaction.eventmodel.TransactionEvent;
import com.mybank.servicetransaction.model.Transaction;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TransactionHelper {

  public static TransactionEvent toTransactionEvent(Transaction transaction) {
    return TransactionEvent.builder()
      .transactionId(transaction.getTransactionId())
      .amount(transaction.getAmount())
      .currency(transaction.getCurrency())
      .note(transaction.getNote())
      .status(transaction.getStatus())
      .source(TransactionEvent.AccountDetail.builder()
        .accountNumber(transaction.getSource().getAccountNumber())
        .accountHolderName(transaction.getSource().getAccountHolderName())
        .accountProvider(transaction.getSource().getAccountProvider())
        .transactionType(transaction.getSource().getTransactionType())
        .build())
      .destination(TransactionEvent.AccountDetail.builder()
        .accountNumber(transaction.getDestination().getAccountNumber())
        .accountHolderName(transaction.getDestination().getAccountHolderName())
        .accountProvider(transaction.getDestination().getAccountProvider())
        .transactionType(transaction.getDestination().getTransactionType())
        .build())
      .timestamp(transaction.getTimestamp())
      .build();
  }

  public static Transaction toTransaction(TransactionEvent event) {
    return Transaction.builder()
      .transactionId(event.getTransactionId())
      .amount(event.getAmount())
      .currency(event.getCurrency())
      .note(event.getNote())
      .status(event.getStatus())
      .statusDetail(event.getStatusDetail())
      .source(Transaction.AccountDetail.builder()
        .accountNumber(event.getSource().getAccountNumber())
        .accountHolderName(event.getSource().getAccountHolderName())
        .accountProvider(event.getSource().getAccountProvider())
        .transactionType(event.getSource().getTransactionType())
        .build())
      .destination(Transaction.AccountDetail.builder()
        .accountNumber(event.getDestination().getAccountNumber())
        .accountHolderName(event.getDestination().getAccountHolderName())
        .accountProvider(event.getDestination().getAccountProvider())
        .transactionType(event.getDestination().getTransactionType())
        .build())
      .timestamp(event.getTimestamp())
      .build();
  }
}
