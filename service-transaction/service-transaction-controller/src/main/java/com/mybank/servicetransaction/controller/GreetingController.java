package com.mybank.servicetransaction.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class GreetingController {

  @GetMapping("/greetings")
  Mono<String> greet() {
    return Mono.just("hello");
  }
}
