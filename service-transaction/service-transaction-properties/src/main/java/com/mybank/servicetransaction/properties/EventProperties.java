package com.mybank.servicetransaction.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties("event.topic")
public class EventProperties {
  private OutboundEvent outbound;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class OutboundEvent {
    private List<String> transactions = new ArrayList<>();
  }
}
