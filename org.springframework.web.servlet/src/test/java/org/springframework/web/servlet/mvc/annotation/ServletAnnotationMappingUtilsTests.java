package org.springframework.web.servlet.mvc.annotation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

/** @author Arjen Poutsma */
public class ServletAnnotationMappingUtilsTests {

	@Test
	public void checkRequestMethodMatch() {
		RequestMethod[] methods = new RequestMethod[]{RequestMethod.GET, RequestMethod.POST};
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		boolean result = ServletAnnotationMappingUtils.checkRequestMethod(methods, request);
		assertTrue("Invalid request method result", result);
	}

	@Test
	public void checkRequestMethodNoMatch() {
		RequestMethod[] methods = new RequestMethod[]{RequestMethod.GET, RequestMethod.POST};
		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/");
		boolean result = ServletAnnotationMappingUtils.checkRequestMethod(methods, request);
		assertFalse("Invalid request method result", result);
	}

}
