package org.springframework.web.context.request;

import java.util.Iterator;

import junit.framework.TestCase;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit test for the {@link HttpServletRequestParameterMap} class.
 * 
 * @author Ulrik Sandberg
 */
public class NativeWebRequestParameterMapTests extends TestCase {

	private NativeWebRequestParameterMap tested;

	private MockHttpServletRequest request;

	protected void setUp() throws Exception {
		super.setUp();
		request = new MockHttpServletRequest();
		tested = new NativeWebRequestParameterMap(new ServletWebRequest(request));
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		request = null;
		tested = null;
	}

	public void testGetAttribute() {
		request.setParameter("Some param", "Some value");
		// perform test
		Object result = tested.getAttribute("Some param");
		assertEquals("Some value", result);
	}

	public void testSetAttribute() {
		// perform test
		try {
			tested.setAttribute("Some key", "Some value");
			fail("UnsupportedOperationException expected");
		} catch (UnsupportedOperationException expected) {
			// expected
		}
	}

	public void testRemoveAttribute() {
		request.setParameter("Some param", "Some value");
		// perform test
		try {
			tested.removeAttribute("Some param");
			fail("UnsupportedOperationException expected");
		} catch (UnsupportedOperationException expected) {
			// expected
		}
	}

	public void testGetAttributeNames() {
		request.setParameter("Some param", "Some value");
		// perform test
		Iterator names = tested.getAttributeNames();
		assertNotNull("Null result unexpected", names);
		assertTrue("More elements", names.hasNext());
		String name = (String) names.next();
		assertEquals("Some param", name);
	}
}