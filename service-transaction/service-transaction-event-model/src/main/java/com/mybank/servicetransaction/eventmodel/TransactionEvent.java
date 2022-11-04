package com.mybank.servicetransaction.eventmodel;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mybank.servicetransaction.enumeration.TransactionStatus;
import com.mybank.servicetransaction.enumeration.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionEvent implements EventTransferAble {
  private UUID transactionId;
  private long amount;
  private String currency;
  private String note;
  private TransactionStatus status;
  private String statusDetail;
  private AccountDetail source;
  private AccountDetail destination;
  private long timestamp;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AccountDetail {
    private String accountHolderName;
    private String accountNumber;
    private String accountProvider;
    private TransactionType transactionType;
  }
}
