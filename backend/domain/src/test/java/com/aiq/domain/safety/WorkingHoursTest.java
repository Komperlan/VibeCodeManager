package com.aiq.domain.safety;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class WorkingHoursTest {

    @Test
    void shouldContainTimeInRegularInterval() {
        WorkingHours workingHours = new WorkingHours(
            LocalTime.of(10, 0),
            LocalTime.of(18, 0),
            ZoneId.of("UTC")
        );

        assertThat(workingHours.contains(LocalTime.of(12, 0))).isTrue();
        assertThat(workingHours.contains(LocalTime.of(19, 0))).isFalse();
    }

    @Test
    void shouldContainTimeInIntervalAcrossMidnight() {
        WorkingHours workingHours = new WorkingHours(
            LocalTime.of(22, 0),
            LocalTime.of(2, 0),
            ZoneId.of("UTC")
        );

        assertThat(workingHours.contains(LocalTime.of(23, 0))).isTrue();
        assertThat(workingHours.contains(LocalTime.of(1, 0))).isTrue();
        assertThat(workingHours.contains(LocalTime.of(12, 0))).isFalse();
    }

    @Test
    void shouldTreatSameStartAndEndAsFullDay() {
        WorkingHours workingHours = new WorkingHours(
            LocalTime.NOON,
            LocalTime.NOON,
            ZoneId.of("UTC")
        );

        assertThat(workingHours.isFullDay()).isTrue();
        assertThat(workingHours.contains(LocalTime.MIDNIGHT)).isTrue();
    }
}
