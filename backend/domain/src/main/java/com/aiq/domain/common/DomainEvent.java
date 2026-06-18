package com.aiq.domain.common;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
