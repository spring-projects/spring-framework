/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.method.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;

/**
 * Test fixture for {@link HandlerMethodArgumentResolverContainer} unit tests.
 * 
 * @author Rossen Stoyanchev
 */
public class HandlerMethodArgumentResolverContainerTests {

	private HandlerMethodArgumentResolverContainer container;

	private MethodParameter paramInteger;

	private MethodParameter paramString;

	@Before
	public void setUp() throws Exception {
		this.container = new HandlerMethodArgumentResolverContainer();

		Method method = getClass().getDeclaredMethod("handle", Integer.class, String.class);
		this.paramInteger = new MethodParameter(method, 0);
		this.paramString = new MethodParameter(method, 1);
	}
	
	@Test
	public void supportsParameter() throws Exception {
		registerResolver(Integer.class, null, false);
		
		assertTrue(this.container.supportsParameter(paramInteger));
		assertFalse(this.container.supportsParameter(paramString));
	}
	
	@Test
	public void resolveArgument() throws Exception {
		registerResolver(Integer.class, Integer.valueOf(55), false);
		Object resolvedValue = this.container.resolveArgument(paramInteger, null, null, null);

		assertEquals(Integer.valueOf(55), resolvedValue);
	}
	
	@Test
	public void resolveArgumentMultipleResolvers() throws Exception {
		registerResolver(Integer.class, Integer.valueOf(1), false);
		registerResolver(Integer.class, Integer.valueOf(2), false);
		Object resolvedValue = this.container.resolveArgument(paramInteger, null, null, null);

		assertEquals("Didn't use the first registered resolver", Integer.valueOf(1), resolvedValue);
	}
	
	@Test(expected=IllegalStateException.class)
	public void noSuitableArgumentResolver() throws Exception {
		this.container.resolveArgument(paramString, null, null, null);
	}

	@Test
	public void argResolverUsesResponse() throws Exception {
		registerResolver(Integer.class, null, true);
		assertTrue(this.container.usesResponseArgument(paramInteger));
	}

	@Test
	public void argResolverDoesntUseResponse() throws Exception {
		registerResolver(Integer.class, null, false);
		assertFalse(this.container.usesResponseArgument(paramInteger));
	}

	protected StubArgumentResolver registerResolver(Class<?> supportedType, Object stubValue, boolean usesResponse) {
		StubArgumentResolver resolver = new StubArgumentResolver(supportedType, stubValue, usesResponse);
		this.container.registerArgumentResolver(resolver);
		return resolver;
	}

	@SuppressWarnings("unused")
	private void handle(Integer arg1, String arg2) {
	}

}
