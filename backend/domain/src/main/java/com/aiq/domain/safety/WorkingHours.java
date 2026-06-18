package com.aiq.domain.safety;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

public record WorkingHours(
    LocalTime from,
    LocalTime to,
    ZoneId zoneId
) {

    public WorkingHours {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(zoneId, "zoneId must not be null");
    }

    public boolean contains(Instant instant) {
        Objects.requireNonNull(instant, "instant must not be null");
        return contains(instant.atZone(zoneId).toLocalTime());
    }

    public boolean contains(LocalTime time) {
        Objects.requireNonNull(time, "time must not be null");

        if (isFullDay()) {
            return true;
        }

        if (from.isBefore(to)) {
            return !time.isBefore(from) && !time.isAfter(to);
        }

        return !time.isBefore(from) || !time.isAfter(to);
    }

    public boolean isFullDay() {
        return from.equals(to);
    }
}
