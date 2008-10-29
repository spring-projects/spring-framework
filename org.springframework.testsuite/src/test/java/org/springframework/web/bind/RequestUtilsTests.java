/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.bind;

import javax.servlet.ServletException;

import junit.framework.TestCase;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Juergen Hoeller
 * @since 06.08.2003
 */
public class RequestUtilsTests extends TestCase {

	public void testRejectMethod() throws ServletRequestBindingException {
		String methodGet = "GET";
		String methodPost = "POST";

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(methodPost);

		try {
			RequestUtils.rejectRequestMethod(request, methodGet);
		} catch (ServletException ex) {
			fail("Shouldn't have thrown ServletException");
		}
		try {
			RequestUtils.rejectRequestMethod(request, methodPost);
			fail("Should have thrown ServletException");
		} catch (ServletException ex) {
		}
	}

	public void testIntParameter() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param1", "5");
		request.addParameter("param2", "e");
		request.addParameter("paramEmpty", "");

		assertEquals(RequestUtils.getIntParameter(request, "param1"), new Integer(5));
		assertEquals(RequestUtils.getIntParameter(request, "param1", 6), 5);
		assertEquals(RequestUtils.getRequiredIntParameter(request, "param1"), 5);

		assertEquals(RequestUtils.getIntParameter(request, "param2", 6), 6);
		try {
			RequestUtils.getRequiredIntParameter(request, "param2");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}

		assertEquals(RequestUtils.getIntParameter(request, "param3"), null);
		assertEquals(RequestUtils.getIntParameter(request, "param3", 6), 6);
		try {
			RequestUtils.getRequiredIntParameter(request, "param3");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}

		try {
			RequestUtils.getRequiredIntParameter(request, "paramEmpty");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}
	}

	public void testIntParameters() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param", new String[] {"1", "2", "3"});

		request.addParameter("param2", "1");
		request.addParameter("param2", "2");
		request.addParameter("param2", "bogus");

		int[] array = new int[] { 1, 2, 3 };
		int[] values = RequestUtils.getRequiredIntParameters(request, "param");
		assertEquals(3, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i]);
		}

		try {
			RequestUtils.getRequiredIntParameters(request, "param2");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}

	}

	public void testLongParameter() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param1", "5");
		request.addParameter("param2", "e");
		request.addParameter("paramEmpty", "");

		assertEquals(RequestUtils.getLongParameter(request, "param1"), new Long(5L));
		assertEquals(RequestUtils.getLongParameter(request, "param1", 6L), 5L);
		assertEquals(RequestUtils.getRequiredIntParameter(request, "param1"), 5L);

		assertEquals(RequestUtils.getLongParameter(request, "param2", 6L), 6L);
		try {
			RequestUtils.getRequiredLongParameter(request, "param2");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}

		assertEquals(RequestUtils.getLongParameter(request, "param3"), null);
		assertEquals(RequestUtils.getLongParameter(request, "param3", 6L), 6L);
		try {
			RequestUtils.getRequiredLongParameter(request, "param3");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}

		try {
			RequestUtils.getRequiredLongParameter(request, "paramEmpty");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}
	}

	public void testLongParameters() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("param", new String[] {"1", "2", "3"});

		request.setParameter("param2", "0");
		request.setParameter("param2", "1");
		request.addParameter("param2", "2");
		request.addParameter("param2", "bogus");

		long[] array = new long[] { 1L, 2L, 3L };
		long[] values = RequestUtils.getRequiredLongParameters(request, "param");
		assertEquals(3, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i]);
		}

		try {
			RequestUtils.getRequiredLongParameters(request, "param2");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}

		request.setParameter("param2", new String[] {"1", "2"});
		values = RequestUtils.getRequiredLongParameters(request, "param2");
		assertEquals(2, values.length);
		assertEquals(1, values[0]);
		assertEquals(2, values[1]);

		request.removeParameter("param2");
		try {
			RequestUtils.getRequiredLongParameters(request, "param2");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}
	}

	public void testFloatParameter() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param1", "5.5");
		request.addParameter("param2", "e");
		request.addParameter("paramEmpty", "");

		assertTrue(RequestUtils.getFloatParameter(request, "param1").equals(new Float(5.5f)));
		assertTrue(RequestUtils.getFloatParameter(request, "param1", 6.5f) == 5.5f);
		assertTrue(RequestUtils.getRequiredFloatParameter(request, "param1") == 5.5f);

		assertTrue(RequestUtils.getFloatParameter(request, "param2", 6.5f) == 6.5f);
		try {
			RequestUtils.getRequiredFloatParameter(request, "param2");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}

		assertTrue(RequestUtils.getFloatParameter(request, "param3") == null);
		assertTrue(RequestUtils.getFloatParameter(request, "param3", 6.5f) == 6.5f);
		try {
			RequestUtils.getRequiredFloatParameter(request, "param3");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}

		try {
			RequestUtils.getRequiredFloatParameter(request, "paramEmpty");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}
	}

	public void testFloatParameters() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param", new String[] {"1.5", "2.5", "3"});

		request.addParameter("param2", "1.5");
		request.addParameter("param2", "2");
		request.addParameter("param2", "bogus");

		float[] array = new float[] { 1.5F, 2.5F, 3 };
		float[] values = RequestUtils.getRequiredFloatParameters(request, "param");
		assertEquals(3, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i], 0);
		}

		try {
			RequestUtils.getRequiredFloatParameters(request, "param2");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}
	}

	public void testDoubleParameter() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param1", "5.5");
		request.addParameter("param2", "e");
		request.addParameter("paramEmpty", "");

		assertTrue(RequestUtils.getDoubleParameter(request, "param1").equals(new Double(5.5)));
		assertTrue(RequestUtils.getDoubleParameter(request, "param1", 6.5) == 5.5);
		assertTrue(RequestUtils.getRequiredDoubleParameter(request, "param1") == 5.5);

		assertTrue(RequestUtils.getDoubleParameter(request, "param2", 6.5) == 6.5);
		try {
			RequestUtils.getRequiredDoubleParameter(request, "param2");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}

		assertTrue(RequestUtils.getDoubleParameter(request, "param3") == null);
		assertTrue(RequestUtils.getDoubleParameter(request, "param3", 6.5) == 6.5);
		try {
			RequestUtils.getRequiredDoubleParameter(request, "param3");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}

		try {
			RequestUtils.getRequiredDoubleParameter(request, "paramEmpty");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}
	}

	public void testDoubleParameters() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param", new String[] {"1.5", "2.5", "3"});

		request.addParameter("param2", "1.5");
		request.addParameter("param2", "2");
		request.addParameter("param2", "bogus");

		double[] array = new double[] { 1.5, 2.5, 3 };
		double[] values = RequestUtils.getRequiredDoubleParameters(request, "param");
		assertEquals(3, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i], 0);
		}

		try {
			RequestUtils.getRequiredDoubleParameters(request, "param2");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}
	}

	public void testBooleanParameter() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param1", "true");
		request.addParameter("param2", "e");
		request.addParameter("param4", "yes");
		request.addParameter("param5", "1");
		request.addParameter("paramEmpty", "");

		assertTrue(RequestUtils.getBooleanParameter(request, "param1").equals(Boolean.TRUE));
		assertTrue(RequestUtils.getBooleanParameter(request, "param1", false));
		assertTrue(RequestUtils.getRequiredBooleanParameter(request, "param1"));

		assertFalse(RequestUtils.getBooleanParameter(request, "param2", true));
		assertFalse(RequestUtils.getRequiredBooleanParameter(request, "param2"));

		assertTrue(RequestUtils.getBooleanParameter(request, "param3") == null);
		assertTrue(RequestUtils.getBooleanParameter(request, "param3", true));
		try {
			RequestUtils.getRequiredBooleanParameter(request, "param3");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}

		assertTrue(RequestUtils.getBooleanParameter(request, "param4", false));
		assertTrue(RequestUtils.getRequiredBooleanParameter(request, "param4"));

		assertTrue(RequestUtils.getBooleanParameter(request, "param5", false));
		assertTrue(RequestUtils.getRequiredBooleanParameter(request, "param5"));
		try {
			RequestUtils.getRequiredBooleanParameter(request, "paramEmpty");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}
	}

	public void testBooleanParameters() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param", new String[] {"true", "yes", "off", "1", "bogus"});

		request.addParameter("param2", "false");
		request.addParameter("param2", "true");
		request.addParameter("param2", "");

		boolean[] array = new boolean[] { true, true, false, true, false };
		boolean[] values = RequestUtils.getRequiredBooleanParameters(request, "param");
		assertEquals(5, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i]);
		}

		try {
			RequestUtils.getRequiredBooleanParameters(request, "param2");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}
	}

	public void testStringParameter() throws ServletRequestBindingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("param1", "str");
		request.addParameter("paramEmpty", "");

		assertEquals(RequestUtils.getStringParameter(request, "param1"), "str");
		assertEquals(RequestUtils.getStringParameter(request, "param1", "string"), "str");
		assertEquals(RequestUtils.getRequiredStringParameter(request, "param1"), "str");

		assertEquals(RequestUtils.getStringParameter(request, "param3"), null);
		assertEquals(RequestUtils.getStringParameter(request, "param3", "string"), "string");
		try {
			RequestUtils.getRequiredStringParameter(request, "param3");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}

		try {
			RequestUtils.getStringParameter(request, "paramEmpty");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}
		try {
			RequestUtils.getRequiredStringParameter(request, "paramEmpty");
			fail("Should have thrown ServletRequestBindingException");
		}
		catch (ServletRequestBindingException ex) {
			// expected
		}
	}

}
