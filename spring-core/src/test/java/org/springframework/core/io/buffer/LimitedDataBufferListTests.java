/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.buffer;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link LimitedDataBufferList}.
 *
 * @author Rossen Stoyanchev
 * @since 5.1.11
 */
class LimitedDataBufferListTests {

	@Test
	void limitEnforced() {
		LimitedDataBufferList list = new LimitedDataBufferList(5);

		assertThatThrownBy(() -> list.add(toDataBuffer("123456"))).isInstanceOf(DataBufferLimitException.class);
		assertThat(list).isEmpty();
	}

	@Test
	void limitIgnored() {
		new LimitedDataBufferList(-1).add(toDataBuffer("123456"));
	}

	@Test
	void clearResetsCount() {
		LimitedDataBufferList list = new LimitedDataBufferList(5);
		list.add(toDataBuffer("12345"));
		list.clear();
		list.add(toDataBuffer("12345"));
	}


	private static DataBuffer toDataBuffer(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
	}

}
