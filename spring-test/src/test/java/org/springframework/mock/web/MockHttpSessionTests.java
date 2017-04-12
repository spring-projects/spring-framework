/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MockHttpSession}.
 *
 * @author Sam Brannen
 * @since 3.2
 */
public class MockHttpSessionTests {

	private final MockHttpSession session = new MockHttpSession();


	@Test
	public void invalidateOnce() {
		assertFalse(session.isInvalid());
		session.invalidate();
		assertTrue(session.isInvalid());
	}

	@Test(expected = IllegalStateException.class)
	public void invalidateTwice() {
		session.invalidate();
		session.invalidate();
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void getCreationTimeOnInvalidatedSession() {
		session.invalidate();
		session.getCreationTime();
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void getLastAccessedTimeOnInvalidatedSession() {
		session.invalidate();
		session.getLastAccessedTime();
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void getAttributeOnInvalidatedSession() {
		session.invalidate();
		session.getAttribute("foo");
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void getAttributeNamesOnInvalidatedSession() {
		session.invalidate();
		session.getAttributeNames();
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void getValueOnInvalidatedSession() {
		session.invalidate();
		session.getValue("foo");
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void getValueNamesOnInvalidatedSession() {
		session.invalidate();
		session.getValueNames();
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void setAttributeOnInvalidatedSession() {
		session.invalidate();
		session.setAttribute("name", "value");
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void putValueOnInvalidatedSession() {
		session.invalidate();
		session.putValue("name", "value");
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void removeAttributeOnInvalidatedSession() {
		session.invalidate();
		session.removeAttribute("name");
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void removeValueOnInvalidatedSession() {
		session.invalidate();
		session.removeValue("name");
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void isNewOnInvalidatedSession() {
		session.invalidate();
		session.isNew();
	}

}
