/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 * @since 2.0
 */
class ParseStateTests {

	@Test
	void testSimple() {
		MockEntry entry = new MockEntry();

		ParseState parseState = new ParseState();
		parseState.push(entry);
		assertThat(parseState.peek()).as("Incorrect peek value.").isEqualTo(entry);
		parseState.pop();
		assertThat(parseState.peek()).as("Should get null on peek()").isNull();
	}

	@Test
	void testNesting() {
		MockEntry one = new MockEntry();
		MockEntry two = new MockEntry();
		MockEntry three = new MockEntry();

		ParseState parseState = new ParseState();
		parseState.push(one);
		assertThat(parseState.peek()).isEqualTo(one);
		parseState.push(two);
		assertThat(parseState.peek()).isEqualTo(two);
		parseState.push(three);
		assertThat(parseState.peek()).isEqualTo(three);

		parseState.pop();
		assertThat(parseState.peek()).isEqualTo(two);
		parseState.pop();
		assertThat(parseState.peek()).isEqualTo(one);
	}

	@Test
	void testSnapshot() {
		MockEntry entry = new MockEntry();

		ParseState original = new ParseState();
		original.push(entry);

		ParseState snapshot = original.snapshot();
		original.push(new MockEntry());
		assertThat(snapshot.peek()).as("Snapshot should not have been modified.").isEqualTo(entry);
	}


	private static class MockEntry implements ParseState.Entry {

	}

}
