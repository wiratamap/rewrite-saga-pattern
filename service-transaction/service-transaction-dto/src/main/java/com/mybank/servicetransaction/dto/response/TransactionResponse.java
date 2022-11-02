package com.mybank.servicetransaction.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mybank.servicetransaction.enumeration.TransactionStatus;
import com.mybank.servicetransaction.enumeration.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {
  private UUID transactionId;
  private long amount;
  private String currency;
  private String note;
  private TransactionStatus status;
  private String statusDetail;
  private TransactionDetailResponse source;
  private TransactionDetailResponse destination;
  private long timestamp;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TransactionDetailResponse {
    private String accountHolderName;
    private String accountNumber;
    private String accountProvider;
    private TransactionType transactionType;
  }
}
