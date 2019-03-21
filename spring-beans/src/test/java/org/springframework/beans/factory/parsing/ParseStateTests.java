/*
 * Copyright 2002-2006 the original author or authors.
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Chris Beams
 * @since 2.0
 */
public class ParseStateTests {

	@Test
	public void testSimple() throws Exception {
		MockEntry entry = new MockEntry();

		ParseState parseState = new ParseState();
		parseState.push(entry);
		assertEquals("Incorrect peek value.", entry, parseState.peek());
		parseState.pop();
		assertNull("Should get null on peek()", parseState.peek());
	}

	@Test
	public void testNesting() throws Exception {
		MockEntry one = new MockEntry();
		MockEntry two = new MockEntry();
		MockEntry three = new MockEntry();

		ParseState parseState = new ParseState();
		parseState.push(one);
		assertEquals(one, parseState.peek());
		parseState.push(two);
		assertEquals(two, parseState.peek());
		parseState.push(three);
		assertEquals(three, parseState.peek());

		parseState.pop();
		assertEquals(two, parseState.peek());
		parseState.pop();
		assertEquals(one, parseState.peek());
	}

	@Test
	public void testSnapshot() throws Exception {
		MockEntry entry = new MockEntry();

		ParseState original = new ParseState();
		original.push(entry);

		ParseState snapshot = original.snapshot();
		original.push(new MockEntry());
		assertEquals("Snapshot should not have been modified.", entry, snapshot.peek());
	}


	private static class MockEntry implements ParseState.Entry {

	}

}
