package com.mybank.serviceaccount.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
  private String cif;
  private String accountNumber;
  private String cardHolderName;
  private String cardProvider;
  private String cardNumber;
  private Long cardExpirationDate;
  private Long cardCreationDate;
  private String cvvCode;
  private long balance;
  private long timestamp;
}
