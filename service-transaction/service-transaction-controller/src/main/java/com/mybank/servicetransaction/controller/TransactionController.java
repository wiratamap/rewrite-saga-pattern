package com.mybank.servicetransaction.controller;

import com.mybank.servicetransaction.dto.request.TransactionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mybank.servicetransaction.dto.response.TransactionResponse;
import com.mybank.servicetransaction.service.TransactionService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionService transactionService;

  @PostMapping("/transactions")
  public Mono<ResponseEntity<TransactionResponse>> create(@Valid @RequestBody TransactionRequest request) {
    return transactionService.create(request)
      .map(response -> ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response))
      .subscribeOn(Schedulers.boundedElastic());
  }
}
