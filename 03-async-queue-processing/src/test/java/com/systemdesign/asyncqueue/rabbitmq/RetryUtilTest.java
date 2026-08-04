package com.systemdesign.asyncqueue.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Direct JUnit 5 port of src/common/rabbitmq/retry.util.spec.ts. */
class RetryUtilTest {

    @Nested
    class ShouldDeadLetter {

        @Test
        void doesNotDeadLetterWhileAttemptsRemainBelowTheMax() {
            assertThat(RetryUtil.shouldDeadLetter(1, 3)).isFalse();
            assertThat(RetryUtil.shouldDeadLetter(2, 3)).isFalse();
        }

        @Test
        void deadLettersOnceAttemptsReachTheMax() {
            assertThat(RetryUtil.shouldDeadLetter(3, 3)).isTrue();
        }

        @Test
        void deadLettersIfSomehowAlreadyPastTheMax() {
            assertThat(RetryUtil.shouldDeadLetter(4, 3)).isTrue();
        }

        @Test
        void defaultsToTheTopologyWideMaxDeliveryAttemptsWhenNoMaxIsGiven() {
            assertThat(RetryUtil.shouldDeadLetter(Topology.MAX_DELIVERY_ATTEMPTS - 1)).isFalse();
            assertThat(RetryUtil.shouldDeadLetter(Topology.MAX_DELIVERY_ATTEMPTS)).isTrue();
        }
    }

    @Nested
    class NextAttempt {

        @Test
        void startsAtOneWhenThereIsNoRetryCountHeaderYet() {
            assertThat(RetryUtil.nextAttempt(null)).isEqualTo(1);
            assertThat(RetryUtil.nextAttempt(Map.of())).isEqualTo(1);
        }

        @Test
        void incrementsTheExistingRetryCountHeader() {
            assertThat(RetryUtil.nextAttempt(Map.of(Topology.RETRY_COUNT_HEADER, 1))).isEqualTo(2);
            assertThat(RetryUtil.nextAttempt(Map.of(Topology.RETRY_COUNT_HEADER, 2))).isEqualTo(3);
        }

        @Test
        void ignoresANonNumericHeaderInsteadOfPropagatingGarbage() {
            assertThat(RetryUtil.nextAttempt(Map.of(Topology.RETRY_COUNT_HEADER, "not-a-number"))).isEqualTo(1);
        }
    }
}
