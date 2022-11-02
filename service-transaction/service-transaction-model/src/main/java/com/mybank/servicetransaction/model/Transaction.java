package com.mybank.servicetransaction.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mybank.servicetransaction.enumeration.TransactionStatus;
import com.mybank.servicetransaction.enumeration.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transaction {
  private UUID transactionId;
  private long amount;
  private String currency;
  private String note;
  private TransactionStatus status;
  private String statusDetail;
  private DestinationDetail source;
  private DestinationDetail destination;
  private long timestamp;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class DestinationDetail {
    private String accountHolderName;
    private String accountNumber;
    private String accountProvider;
    private TransactionType transactionType;
  }
}
