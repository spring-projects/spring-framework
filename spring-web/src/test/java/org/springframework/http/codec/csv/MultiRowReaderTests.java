/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.codec.csv;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link MultiRowReader}.
 */
class MultiRowReaderTests {

	private final MultiRowReader reader = new MultiRowReader();

	/**
	 * Test for {@link MultiRowReader#size()} and
	 * implicitly for {@link MultiRowReader#addRow(String)}.
	 */
	@Test
	void size() {
		assertThat(reader.size()).isZero();

		reader.addRow("abc");

		assertThat(reader.size()).isEqualTo(1);

		reader.addRow("abc");

		assertThat(reader.size()).isEqualTo(2);
	}

	/**
	 * Test for {@link MultiRowReader#addRow(String)}.
	 */
	@Test
	void add_empty() {
		reader.addRow("");

		assertThat(reader.size()).isZero();
	}

	/**
	 * Test for {@link MultiRowReader#addRow(String)}.
	 */
	@Test
	void add_first() {
		reader.addRow("ab");

		var destination1 = new char[1];
		var count1 = reader.read(destination1, 0, 1);
		assertThat(count1).isEqualTo(1);
		assertThat(destination1).containsExactly('a');
	}

	/**
	 * Test for {@link MultiRowReader#addRow(String)}.
	 * Test that adding a second row does not interrupt a prior read.
	 */
	@Test
	void add_second() {
		reader.addRow("ab");

		var destination1 = new char[1];
		var count1 = reader.read(destination1, 0, 1);
		assertThat(count1).isEqualTo(1);
		assertThat(destination1).containsExactly('a');

		reader.addRow("cd");

		var destination2 = new char[1];
		var count2 = reader.read(destination2, 0, 1);
		assertThat(count2).isEqualTo(1);
		assertThat(destination2).containsExactly('b');

		var destination3 = new char[1];
		var count3 = reader.read(destination3, 0, 1);
		assertThat(count3).isEqualTo(1);
		assertThat(destination3).containsExactly('c');
	}

	/**
	 * Test for {@link MultiRowReader#read(char[], int, int)}.
	 * Test that an exception is thrown if the buffer is depleted unplanned.
	 */
	@Test
	void read_empty() {
		assertThatThrownBy(() -> reader.read(new char[1], 0, 1))
				.isInstanceOf(IllegalArgumentException.class);
	}

	/**
	 * Test for {@link MultiRowReader#read(char[], int, int)} and
	 * implicitly for {@link MultiRowReader#close()}.
	 */
	@Test
	void read_end() {
		reader.close();

		assertThat(reader.read(new char[1], 0, 1)).isEqualTo(-1);
	}

	/**
	 * Test for {@link MultiRowReader#read(char[], int, int)} and
	 * implicitly for {@link MultiRowReader#addRow(String)}.
	 */
	@Test
	void read() {
		reader.addRow("abc");
		reader.addRow("d");

		var destination1 = new char[2];
		var count1 = reader.read(destination1, 0, 2);
		assertThat(count1).isEqualTo(2);
		assertThat(destination1).containsExactly('a', 'b');

		var destination2 = new char[2];
		var count2 = reader.read(destination2, 0, 2);
		assertThat(count2).isEqualTo(1);
		assertThat(destination2).containsExactly('c', (char) 0);

		assertThatThrownBy(() -> reader.read(new char[1], 0, 0))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
