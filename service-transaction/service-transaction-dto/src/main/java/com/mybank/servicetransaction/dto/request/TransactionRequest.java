package com.mybank.servicetransaction.dto.request;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {
  @NotNull(message = "sourceAccountNumber cannot be null")
  private String sourceAccountNumber;
  @NotNull(message = "destinationAccountRequest cannot be null")
  private DestinationAccountRequest destinationAccountRequest;
  @NotNull(message = "amount cannot be null")
  private long amount;
  @NotNull(message = "currency cannot be null")
  private String currency;
  private String note;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class DestinationAccountRequest {
    private String accountHolderName;
    private String accountProvider;
    @NotNull(message = "destination accountNumber cannot be null")
    private String accountNumber;
  }
}
