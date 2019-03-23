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

package org.springframework.web.portlet.handler;

import org.junit.Test;

import org.springframework.mock.web.portlet.MockActionRequest;
import org.springframework.mock.web.portlet.MockActionResponse;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Sam Brannen
 */
public class ParameterMappingInterceptorTests {

	private final ParameterMappingInterceptor interceptor = new ParameterMappingInterceptor();

	private final Object handler = new Object();

	private final MockActionRequest request = new MockActionRequest();

	private final MockActionResponse response = new MockActionResponse();


	@Test
	public void defaultParameterMapped() throws Exception {
		String param = ParameterHandlerMapping.DEFAULT_PARAMETER_NAME;
		String value = "someValue";
		request.setParameter(param, value);
		assertNull(response.getRenderParameter(param));
		boolean shouldProceed = interceptor.preHandleAction(request, response, handler);
		assertTrue(shouldProceed);
		assertNotNull(response.getRenderParameter(param));
		assertEquals(value, response.getRenderParameter(param));
	}

	@Test
	public void nonDefaultParameterNotMapped() throws Exception {
		String param = "myParam";
		String value = "someValue";
		request.setParameter(param, value);
		assertNull(response.getRenderParameter(param));
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
		assertNull(response.getRenderParameter(param));
		assertNull(response.getRenderParameter(ParameterHandlerMapping.DEFAULT_PARAMETER_NAME));
	}

	@Test
	public void nonDefaultParameterMappedWhenHandlerMappingProvided() throws Exception {
		String param = "myParam";
		String value = "someValue";
		ParameterHandlerMapping handlerMapping = new ParameterHandlerMapping();
		handlerMapping.setParameterName(param);
		interceptor.setParameterName(param);
		request.setParameter(param, value);
		assertNull(response.getRenderParameter(param));
		boolean shouldProceed = interceptor.preHandleAction(request, response, handler);
		assertTrue(shouldProceed);
		assertNull(response.getRenderParameter(ParameterHandlerMapping.DEFAULT_PARAMETER_NAME));
		assertNotNull(response.getRenderParameter(param));
		assertEquals(value, response.getRenderParameter(param));
	}

	@Test
	public void noEffectForRenderRequest() throws Exception {
		String param = ParameterHandlerMapping.DEFAULT_PARAMETER_NAME;
		String value = "someValue";
		request.setParameter(param, value);
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
	}

	@Test
	public void noParameterValueSetWithDefaultParameterName() throws Exception {
		String param = ParameterHandlerMapping.DEFAULT_PARAMETER_NAME;
		assertNull(response.getRenderParameter(param));
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
		assertNull(response.getRenderParameter(param));
	}

	@Test
	public void noParameterValueSetWithNonDefaultParameterName() throws Exception {
		String param = "myParam";
		assertNull(response.getRenderParameter(param));
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
		assertNull(response.getRenderParameter(param));
	}

	@Test
	public void noParameterValueSetWithNonDefaultParameterNameWhenHandlerMappingProvided() throws Exception {
		String param = "myParam";
		ParameterHandlerMapping handlerMapping = new ParameterHandlerMapping();
		handlerMapping.setParameterName(param);
		interceptor.setParameterName(param);
		assertNull(response.getRenderParameter(param));
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
		assertNull(response.getRenderParameter(param));
	}

}
