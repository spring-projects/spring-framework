/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.portlet.bind;

import junit.framework.TestCase;

import org.springframework.mock.web.portlet.MockPortletRequest;
import org.springframework.util.StopWatch;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 */
public class PortletRequestUtilsTests extends TestCase {

	public void testIntParameter() throws PortletRequestBindingException {
		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("param1", "5");
		request.addParameter("param2", "e");
		request.addParameter("paramEmpty", "");

		assertEquals(PortletRequestUtils.getIntParameter(request, "param1"), new Integer(5));
		assertEquals(PortletRequestUtils.getIntParameter(request, "param1", 6), 5);
		assertEquals(PortletRequestUtils.getRequiredIntParameter(request, "param1"), 5);

		assertEquals(PortletRequestUtils.getIntParameter(request, "param2", 6), 6);
		try {
			PortletRequestUtils.getRequiredIntParameter(request, "param2");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}

		assertEquals(PortletRequestUtils.getIntParameter(request, "param3"), null);
		assertEquals(PortletRequestUtils.getIntParameter(request, "param3", 6), 6);
		try {
			PortletRequestUtils.getRequiredIntParameter(request, "param3");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}

		try {
			PortletRequestUtils.getRequiredIntParameter(request, "paramEmpty");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}
	}

	public void testIntParameters() throws PortletRequestBindingException {
		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("param", new String[] {"1", "2", "3"});

		request.addParameter("param2", "1");
		request.addParameter("param2", "2");
		request.addParameter("param2", "bogus");

		int[] array = new int[] { 1, 2, 3 };
		int[] values = PortletRequestUtils.getRequiredIntParameters(request, "param");
		assertEquals(3, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i]);
		}

		try {
			PortletRequestUtils.getRequiredIntParameters(request, "param2");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}

	}

	public void testLongParameter() throws PortletRequestBindingException {
		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("param1", "5");
		request.addParameter("param2", "e");
		request.addParameter("paramEmpty", "");

		assertEquals(PortletRequestUtils.getLongParameter(request, "param1"), new Long(5L));
		assertEquals(PortletRequestUtils.getLongParameter(request, "param1", 6L), 5L);
		assertEquals(PortletRequestUtils.getRequiredIntParameter(request, "param1"), 5L);
		assertEquals(PortletRequestUtils.getLongParameter(request, "param2", 6L), 6L);
		
		try {
			PortletRequestUtils.getRequiredLongParameter(request, "param2");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}

		assertEquals(PortletRequestUtils.getLongParameter(request, "param3"), null);
		assertEquals(PortletRequestUtils.getLongParameter(request, "param3", 6L), 6L);
		try {
			PortletRequestUtils.getRequiredLongParameter(request, "param3");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}

		try {
			PortletRequestUtils.getRequiredLongParameter(request, "paramEmpty");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}
	}

	public void testLongParameters() throws PortletRequestBindingException {
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter("param", new String[] {"1", "2", "3"});

		request.setParameter("param2", "0");
		request.setParameter("param2", "1");
		request.addParameter("param2", "2");
		request.addParameter("param2", "bogus");

		long[] array = new long[] { 1L, 2L, 3L };
		long[] values = PortletRequestUtils.getRequiredLongParameters(request, "param");
		assertEquals(3, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i]);
		}

		try {
			PortletRequestUtils.getRequiredLongParameters(request, "param2");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}

		request.setParameter("param2", new String[] {"1", "2"});
		values = PortletRequestUtils.getRequiredLongParameters(request, "param2");
		assertEquals(2, values.length);
		assertEquals(1, values[0]);
		assertEquals(2, values[1]);
	}

	public void testFloatParameter() throws PortletRequestBindingException {
		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("param1", "5.5");
		request.addParameter("param2", "e");
		request.addParameter("paramEmpty", "");

		assertTrue(PortletRequestUtils.getFloatParameter(request, "param1").equals(new Float(5.5f)));
		assertTrue(PortletRequestUtils.getFloatParameter(request, "param1", 6.5f) == 5.5f);
		assertTrue(PortletRequestUtils.getRequiredFloatParameter(request, "param1") == 5.5f);

		assertTrue(PortletRequestUtils.getFloatParameter(request, "param2", 6.5f) == 6.5f);
		try {
			PortletRequestUtils.getRequiredFloatParameter(request, "param2");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}

		assertTrue(PortletRequestUtils.getFloatParameter(request, "param3") == null);
		assertTrue(PortletRequestUtils.getFloatParameter(request, "param3", 6.5f) == 6.5f);
		try {
			PortletRequestUtils.getRequiredFloatParameter(request, "param3");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}

		try {
			PortletRequestUtils.getRequiredFloatParameter(request, "paramEmpty");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}
	}

	public void testFloatParameters() throws PortletRequestBindingException {
		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("param", new String[] {"1.5", "2.5", "3"});

		request.addParameter("param2", "1.5");
		request.addParameter("param2", "2");
		request.addParameter("param2", "bogus");

		float[] array = new float[] { 1.5F, 2.5F, 3 };
		float[] values = PortletRequestUtils.getRequiredFloatParameters(request, "param");
		assertEquals(3, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i], 0);
		}

		try {
			PortletRequestUtils.getRequiredFloatParameters(request, "param2");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}
	}

	public void testDoubleParameter() throws PortletRequestBindingException {
		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("param1", "5.5");
		request.addParameter("param2", "e");
		request.addParameter("paramEmpty", "");

		assertTrue(PortletRequestUtils.getDoubleParameter(request, "param1").equals(new Double(5.5)));
		assertTrue(PortletRequestUtils.getDoubleParameter(request, "param1", 6.5) == 5.5);
		assertTrue(PortletRequestUtils.getRequiredDoubleParameter(request, "param1") == 5.5);

		assertTrue(PortletRequestUtils.getDoubleParameter(request, "param2", 6.5) == 6.5);
		try {
			PortletRequestUtils.getRequiredDoubleParameter(request, "param2");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}

		assertTrue(PortletRequestUtils.getDoubleParameter(request, "param3") == null);
		assertTrue(PortletRequestUtils.getDoubleParameter(request, "param3", 6.5) == 6.5);
		try {
			PortletRequestUtils.getRequiredDoubleParameter(request, "param3");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}

		try {
			PortletRequestUtils.getRequiredDoubleParameter(request, "paramEmpty");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}
	}

	public void testDoubleParameters() throws PortletRequestBindingException {
		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("param", new String[] {"1.5", "2.5", "3"});

		request.addParameter("param2", "1.5");
		request.addParameter("param2", "2");
		request.addParameter("param2", "bogus");

		double[] array = new double[] { 1.5, 2.5, 3 };
		double[] values = PortletRequestUtils.getRequiredDoubleParameters(request, "param");
		assertEquals(3, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i], 0);
		}

		try {
			PortletRequestUtils.getRequiredDoubleParameters(request, "param2");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}
	}

	public void testBooleanParameter() throws PortletRequestBindingException {
		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("param1", "true");
		request.addParameter("param2", "e");
		request.addParameter("param4", "yes");
		request.addParameter("param5", "1");
		request.addParameter("paramEmpty", "");

		assertTrue(PortletRequestUtils.getBooleanParameter(request, "param1").equals(Boolean.TRUE));
		assertTrue(PortletRequestUtils.getBooleanParameter(request, "param1", false));
		assertTrue(PortletRequestUtils.getRequiredBooleanParameter(request, "param1"));

		assertFalse(PortletRequestUtils.getBooleanParameter(request, "param2", true));
		assertFalse(PortletRequestUtils.getRequiredBooleanParameter(request, "param2"));

		assertTrue(PortletRequestUtils.getBooleanParameter(request, "param3") == null);
		assertTrue(PortletRequestUtils.getBooleanParameter(request, "param3", true));
		try {
			PortletRequestUtils.getRequiredBooleanParameter(request, "param3");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}

		assertTrue(PortletRequestUtils.getBooleanParameter(request, "param4", false));
		assertTrue(PortletRequestUtils.getRequiredBooleanParameter(request, "param4"));

		assertTrue(PortletRequestUtils.getBooleanParameter(request, "param5", false));
		assertTrue(PortletRequestUtils.getRequiredBooleanParameter(request, "param5"));
		assertFalse(PortletRequestUtils.getRequiredBooleanParameter(request, "paramEmpty"));
	}

	public void testBooleanParameters() throws PortletRequestBindingException {
		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("param", new String[] {"true", "yes", "off", "1", "bogus"});

		request.addParameter("param2", "false");
		request.addParameter("param2", "true");
		request.addParameter("param2", "");

		boolean[] array = new boolean[] { true, true, false, true, false };
		boolean[] values = PortletRequestUtils.getRequiredBooleanParameters(request, "param");
		assertEquals(5, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i]);
		}

		array = new boolean[] { false, true, false };
		values = PortletRequestUtils.getRequiredBooleanParameters(request, "param2");
		assertEquals(array.length, values.length);
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], values[i]);
		}
	}

	public void testStringParameter() throws PortletRequestBindingException {
		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("param1", "str");
		request.addParameter("paramEmpty", "");

		assertEquals("str", PortletRequestUtils.getStringParameter(request, "param1"));
		assertEquals("str", PortletRequestUtils.getStringParameter(request, "param1", "string"));
		assertEquals("str", PortletRequestUtils.getRequiredStringParameter(request, "param1"));

		assertEquals(null, PortletRequestUtils.getStringParameter(request, "param3"));
		assertEquals("string", PortletRequestUtils.getStringParameter(request, "param3", "string"));
		try {
			PortletRequestUtils.getRequiredStringParameter(request, "param3");
			fail("Should have thrown PortletRequestBindingException");
		}
		catch (PortletRequestBindingException ex) {
			// expected
		}

		assertEquals("", PortletRequestUtils.getStringParameter(request, "paramEmpty"));
		assertEquals("", PortletRequestUtils.getRequiredStringParameter(request, "paramEmpty"));
	}

	public void testGetIntParameterWithDefaultValueHandlingIsFastEnough() {
		MockPortletRequest request = new MockPortletRequest();
		StopWatch sw = new StopWatch();
		sw.start();
		for (int i = 0; i < 1000000; i++) {
			PortletRequestUtils.getIntParameter(request, "nonExistingParam", 0);
		}
		sw.stop();
		assertThat(sw.getTotalTimeMillis(), lessThan(250L));
	}

	public void testGetLongParameterWithDefaultValueHandlingIsFastEnough() {
		MockPortletRequest request = new MockPortletRequest();
		StopWatch sw = new StopWatch();
		sw.start();
		for (int i = 0; i < 1000000; i++) {
			PortletRequestUtils.getLongParameter(request, "nonExistingParam", 0);
		}
		sw.stop();
		assertThat(sw.getTotalTimeMillis(), lessThan(250L));
	}

	public void testGetFloatParameterWithDefaultValueHandlingIsFastEnough() {
		MockPortletRequest request = new MockPortletRequest();
		StopWatch sw = new StopWatch();
		sw.start();
		for (int i = 0; i < 1000000; i++) {
			PortletRequestUtils.getFloatParameter(request, "nonExistingParam", 0f);
		}
		sw.stop();
		assertThat(sw.getTotalTimeMillis(), lessThan(250L));
	}

	public void testGetDoubleParameterWithDefaultValueHandlingIsFastEnough() {
		MockPortletRequest request = new MockPortletRequest();
		StopWatch sw = new StopWatch();
		sw.start();
		for (int i = 0; i < 1000000; i++) {
			PortletRequestUtils.getDoubleParameter(request, "nonExistingParam", 0d);
		}
		sw.stop();
		assertThat(sw.getTotalTimeMillis(), lessThan(250L));
	}

	public void testGetBooleanParameterWithDefaultValueHandlingIsFastEnough() {
		MockPortletRequest request = new MockPortletRequest();
		StopWatch sw = new StopWatch();
		sw.start();
		for (int i = 0; i < 1000000; i++) {
			PortletRequestUtils.getBooleanParameter(request, "nonExistingParam", false);
		}
		sw.stop();
		assertThat(sw.getTotalTimeMillis(), lessThan(250L));
	}

	public void testGetStringParameterWithDefaultValueHandlingIsFastEnough() {
		MockPortletRequest request = new MockPortletRequest();
		StopWatch sw = new StopWatch();
		sw.start();
		for (int i = 0; i < 1000000; i++) {
			PortletRequestUtils.getStringParameter(request, "nonExistingParam", "defaultValue");
		}
		sw.stop();
		assertThat(sw.getTotalTimeMillis(), lessThan(250L));
	}

}
