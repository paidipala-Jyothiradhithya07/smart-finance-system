package com.smartfinance.statement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentMethodDetectorTest {

    @Test
    void recognisesTheCommonMethods() {
        assertThat(PaymentMethodDetector.detect("UPI/SWIGGY/123", null)).isEqualTo("UPI");
        assertThat(PaymentMethodDetector.detect("SWIGGY", "UPI2026020501")).isEqualTo("UPI");
        assertThat(PaymentMethodDetector.detect("NEFT-HDFC-SALARY", null)).isEqualTo("NEFT");
        assertThat(PaymentMethodDetector.detect("NETFLIX", "CARD2026021801")).isEqualTo("CARD");
        assertThat(PaymentMethodDetector.detect("ATM WDL MG ROAD", null)).isEqualTo("ATM");
        assertThat(PaymentMethodDetector.detect("CHQ 123456", null)).isEqualTo("CHEQUE");
    }

    @Test
    void doesNotMatchInsideOtherWords() {
        assertThat(PaymentMethodDetector.detect("CARDIOLOGY CLINIC", null)).isNull();
        assertThat(PaymentMethodDetector.detect("POSTPAID RECHARGE", null)).isNull();
        assertThat(PaymentMethodDetector.detect("RENT PAYMENT", "REF2026020301")).isNull();
        assertThat(PaymentMethodDetector.detect(null, null)).isNull();
    }
}