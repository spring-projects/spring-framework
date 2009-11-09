/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mock.web;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 19.02.2006
 */
public class MockServletContextTests {

	@Ignore
	// fails to work under ant after move from .testsuite -> .test
	@Test
	public void testListFiles() {
		MockServletContext sc = new MockServletContext("org/springframework/mock");
		Set<?> paths = sc.getResourcePaths("/web");
		assertNotNull(paths);
		assertTrue(paths.contains("/web/MockServletContextTests.class"));
	}

	@Ignore
	// fails to work under ant after move from .testsuite -> .test
	@Test
	public void testListSubdirectories() {
		MockServletContext sc = new MockServletContext("org/springframework/mock");
		Set<?> paths = sc.getResourcePaths("/");
		assertNotNull(paths);
		assertTrue(paths.contains("/web/"));
	}

	@Test
	public void testListNonDirectory() {
		MockServletContext sc = new MockServletContext("org/springframework/mock");
		Set<?> paths = sc.getResourcePaths("/web/MockServletContextTests.class");
		assertNull(paths);
	}

	@Test
	public void testListInvalidPath() {
		MockServletContext sc = new MockServletContext("org/springframework/mock");
		Set<?> paths = sc.getResourcePaths("/web/invalid");
		assertNull(paths);
	}

	@Test
	public void testGetContext() {
		MockServletContext sc = new MockServletContext();
		MockServletContext sc2 = new MockServletContext();
		sc.setContextPath("/");
		sc.registerContext("/second", sc2);
		assertSame(sc, sc.getContext("/"));
		assertSame(sc2, sc.getContext("/second"));
	}

	@Test
	public void testGetMimeType() {
		MockServletContext sc = new MockServletContext();
		assertEquals("text/html", sc.getMimeType("test.html"));
		assertEquals("image/gif", sc.getMimeType("test.gif"));
	}

	@Test
	public void testMinorVersion() {
		MockServletContext sc = new MockServletContext();
		assertEquals(5, sc.getMinorVersion());
		sc.setMinorVersion(4);
		assertEquals(4, sc.getMinorVersion());
	}

	@Test
	public void testIllegalMinorVersion() {
		MockServletContext sc = new MockServletContext();
		assertEquals(5, sc.getMinorVersion());
		try {
			sc.setMinorVersion(2);
			fail("expected expection for illegal argument");
		}
		catch (IllegalArgumentException iae) {
			// expected
		}
	}
}
