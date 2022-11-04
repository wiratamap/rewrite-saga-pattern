package com.mybank.serviceaccount.model;

import java.util.Set;

import com.mybank.serviceaccount.enumeration.Gender;
import com.mybank.serviceaccount.enumeration.MaritalStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
  private String cif;
  private String name;
  private Gender gender;
  private String email;
  private String phoneNumber;
  private long totalBalance;
  private boolean isPriority;
  private Set<Account> accounts;
  private PersonalInformation personalInformation;
  private long timestamp;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PersonalInformation {
    private Long dateOfBirth;
    private String placeOfBirth;
    private String identityNumber;
    private String religion;
    private String nationality;
    private MaritalStatus maritalStatus;
    private String taxNumber;
    private String identityImagePath;
    private String taxImagePath;
    private String selfieImagePath;
  }
}
