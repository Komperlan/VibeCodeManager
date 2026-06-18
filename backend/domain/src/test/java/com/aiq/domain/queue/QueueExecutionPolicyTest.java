package com.aiq.domain.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.aiq.domain.safety.WorkingHours;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class QueueExecutionPolicyTest {

    @Test
    void shouldCreateDefaultPolicy() {
        QueueExecutionPolicy policy = QueueExecutionPolicy.defaultPolicy();

        assertThat(policy.autoRunMode()).isEqualTo(AutoRunMode.ASK_CONFIRMATION);
        assertThat(policy.maxPromptsPerRun()).isEqualTo(3);
        assertThat(policy.cooldown()).isEqualTo(Duration.ofSeconds(60));
        assertThat(policy.stopOnError()).isTrue();
        assertThat(policy.workingHoursEnabled()).isFalse();
        assertThat(policy.workingHours()).isNull();
    }

    @Test
    void shouldRejectMissingWorkingHoursWhenEnabled() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new QueueExecutionPolicy(
                AutoRunMode.AUTO_RUN,
                1,
                Duration.ZERO,
                true,
                true,
                null
            ))
            .withMessage("Working hours must be set when working hours are enabled");
    }

    @Test
    void shouldAcceptWorkingHoursWhenEnabled() {
        WorkingHours workingHours = new WorkingHours(
            LocalTime.of(9, 0),
            LocalTime.of(18, 0),
            ZoneId.of("UTC")
        );

        QueueExecutionPolicy policy = new QueueExecutionPolicy(
            AutoRunMode.AUTO_RUN,
            1,
            Duration.ZERO,
            true,
            true,
            workingHours
        );

        assertThat(policy.workingHours()).isSameAs(workingHours);
    }
}
