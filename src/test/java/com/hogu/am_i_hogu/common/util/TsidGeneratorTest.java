package com.hogu.am_i_hogu.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class TsidGeneratorTest {

	@Test
	void nextId_returnsUniquePositiveLongIds() {
		TsidGenerator generator = new TsidGenerator();
		Set<Long> ids = new HashSet<>();

		for (int i = 0; i < 1_000; i++) {
			long id = generator.nextId();

			assertThat(id).isPositive();
			assertThat(ids.add(id)).isTrue();
		}
	}
}
