/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.tags.form;

import java.util.stream.IntStream;

import javax.servlet.jsp.PageContext;

import org.junit.Test;

import org.springframework.mock.web.test.MockPageContext;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.0
 */
public class TagIdGeneratorTests {

	@Test
	public void nextId() {
		// Repeat a few times just to be sure...
		IntStream.rangeClosed(1, 5).forEach(i -> assertNextId());
	}

	private void assertNextId() {
		PageContext pageContext = new MockPageContext();
		assertEquals("foo1", TagIdGenerator.nextId("foo", pageContext));
		assertEquals("foo2", TagIdGenerator.nextId("foo", pageContext));
		assertEquals("foo3", TagIdGenerator.nextId("foo", pageContext));
		assertEquals("foo4", TagIdGenerator.nextId("foo", pageContext));
		assertEquals("bar1", TagIdGenerator.nextId("bar", pageContext));
	}

}
