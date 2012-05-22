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

package org.springframework.web.portlet.handler;

import junit.framework.TestCase;

import org.springframework.mock.web.portlet.MockActionRequest;
import org.springframework.mock.web.portlet.MockActionResponse;
import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;

/**
 * @author Mark Fisher
 */
public class ParameterMappingInterceptorTests extends TestCase {

	public void testDefaultParameterMapped() throws Exception {
		ParameterMappingInterceptor interceptor = new ParameterMappingInterceptor();
		Object handler = new Object();
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		String param = ParameterHandlerMapping.DEFAULT_PARAMETER_NAME;
		String value = "someValue";
		request.setParameter(param, value);
		assertNull(response.getRenderParameter(param));
		boolean shouldProceed = interceptor.preHandleAction(request, response, handler);
		assertTrue(shouldProceed);
		assertNotNull(response.getRenderParameter(param));
		assertEquals(value, response.getRenderParameter(param));
	}

	public void testNonDefaultParameterNotMapped() throws Exception {
		ParameterMappingInterceptor interceptor = new ParameterMappingInterceptor();
		Object handler = new Object();
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		String param = "myParam";
		String value = "someValue";
		request.setParameter(param, value);
		assertNull(response.getRenderParameter(param));
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
		assertNull(response.getRenderParameter(param));
		assertNull(response.getRenderParameter(ParameterHandlerMapping.DEFAULT_PARAMETER_NAME));
	}

	public void testNonDefaultParameterMappedWhenHandlerMappingProvided() throws Exception {
		String param = "myParam";
		String value = "someValue";
		ParameterHandlerMapping handlerMapping = new ParameterHandlerMapping();
		handlerMapping.setParameterName(param);
		ParameterMappingInterceptor interceptor = new ParameterMappingInterceptor();
		interceptor.setParameterName(param);
		Object handler = new Object();
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setParameter(param, value);
		assertNull(response.getRenderParameter(param));
		boolean shouldProceed = interceptor.preHandleAction(request, response, handler);
		assertTrue(shouldProceed);
		assertNull(response.getRenderParameter(ParameterHandlerMapping.DEFAULT_PARAMETER_NAME));
		assertNotNull(response.getRenderParameter(param));
		assertEquals(value, response.getRenderParameter(param));
	}

	public void testNoEffectForRenderRequest() throws Exception {
		ParameterMappingInterceptor interceptor = new ParameterMappingInterceptor();
		Object handler = new Object();
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		String param = ParameterHandlerMapping.DEFAULT_PARAMETER_NAME;
		String value = "someValue";
		request.setParameter(param, value);
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
	}

	public void testNoParameterValueSetWithDefaultParameterName() throws Exception {
		ParameterMappingInterceptor interceptor = new ParameterMappingInterceptor();
		Object handler = new Object();
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		String param = ParameterHandlerMapping.DEFAULT_PARAMETER_NAME;
		assertNull(response.getRenderParameter(param));
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
		assertNull(response.getRenderParameter(param));
	}

	public void testNoParameterValueSetWithNonDefaultParameterName() throws Exception {
		ParameterMappingInterceptor interceptor = new ParameterMappingInterceptor();
		Object handler = new Object();
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		String param = "myParam";
		assertNull(response.getRenderParameter(param));
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
		assertNull(response.getRenderParameter(param));
	}

	public void testNoParameterValueSetWithNonDefaultParameterNameWhenHandlerMappingProvided() throws Exception {
		String param = "myParam";
		ParameterHandlerMapping handlerMapping = new ParameterHandlerMapping();
		handlerMapping.setParameterName(param);
		ParameterMappingInterceptor interceptor = new ParameterMappingInterceptor();
		interceptor.setParameterName(param);
		Object handler = new Object();
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		assertNull(response.getRenderParameter(param));
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
		assertNull(response.getRenderParameter(param));
	}

}
