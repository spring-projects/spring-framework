/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io.buffer;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LimitedDataBufferList}.
 * @author Rossen Stoyanchev
 * @since 5.1.11
 */
public class LimitedDataBufferListTests {

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
