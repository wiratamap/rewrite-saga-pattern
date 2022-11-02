package com.mybank.servicetransaction.eventmodel;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent<T extends EventTransferAble> {
  private UUID eventId;
  private long timestamp;
  private T payload;
}
