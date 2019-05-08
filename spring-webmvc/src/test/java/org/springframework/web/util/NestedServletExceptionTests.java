/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.util;

import org.junit.Test;

import org.springframework.core.NestedExceptionUtils;

import static org.junit.Assert.assertEquals;

public class NestedServletExceptionTests {

	@Test
	public void testNestedServletExceptionString() {
		NestedServletException exception = new NestedServletException("foo");
		assertEquals("foo", exception.getMessage());
	}

	@Test
	public void testNestedServletExceptionStringThrowable() {
		Throwable cause = new RuntimeException();
		NestedServletException exception = new NestedServletException("foo", cause);
		assertEquals(NestedExceptionUtils.buildMessage("foo", cause), exception.getMessage());
		assertEquals(cause, exception.getCause());
	}

	@Test
	public void testNestedServletExceptionStringNullThrowable() {
		// This can happen if someone is sloppy with Throwable causes...
		NestedServletException exception = new NestedServletException("foo", null);
		assertEquals("foo", exception.getMessage());
	}

}
