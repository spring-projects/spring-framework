package org.springframework.web.util;

import org.junit.Test;

import org.springframework.core.NestedExceptionUtils;

import static org.junit.Assert.*;

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
