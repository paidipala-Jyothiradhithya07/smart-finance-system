package com.smartfinance.repository.spec;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class TransactionSpecificationsTest {
	@Test
    void escapeLike_makesWildcardsMatchThemselves() {
        assertThat(TransactionSpecifications.escapeLike("50%_off!"))
                .isEqualTo("50!%!_off!!");
    }

    @Test
    void escapeLike_leavesNormalTextUnchanged() {
        assertThat(TransactionSpecifications.escapeLike("swiggy order 12"))
                .isEqualTo("swiggy order 12");
    }
}
