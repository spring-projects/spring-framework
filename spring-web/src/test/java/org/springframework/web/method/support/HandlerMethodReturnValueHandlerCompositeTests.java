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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;

/**
 * Test fixture with {@link HandlerMethodReturnValueHandlerComposite}.
 *
 * @author Rossen Stoyanchev
 */
public class HandlerMethodReturnValueHandlerCompositeTests {

	private HandlerMethodReturnValueHandlerComposite handlers;

	ModelAndViewContainer mavContainer;

	private MethodParameter paramInt;

	private MethodParameter paramStr;

	@Before
	public void setUp() throws Exception {
		handlers = new HandlerMethodReturnValueHandlerComposite();
		mavContainer = new ModelAndViewContainer();
		paramInt = new MethodParameter(getClass().getDeclaredMethod("handleInteger"), -1);
		paramStr = new MethodParameter(getClass().getDeclaredMethod("handleString"), -1);
	}

	@Test
	public void supportsReturnType() throws Exception {
		registerHandler(Integer.class);

		assertTrue(this.handlers.supportsReturnType(paramInt));
		assertFalse(this.handlers.supportsReturnType(paramStr));
	}

	@Test
	public void handleReturnValue() throws Exception {
		StubReturnValueHandler handler = registerHandler(Integer.class);
		this.handlers.handleReturnValue(Integer.valueOf(55), paramInt, mavContainer, null);

		assertEquals(Integer.valueOf(55), handler.getReturnValue());
	}

	@Test
	public void handleReturnValueMultipleHandlers() throws Exception {
		StubReturnValueHandler h1 = registerHandler(Integer.class);
		StubReturnValueHandler h2 = registerHandler(Integer.class);
		this.handlers.handleReturnValue(Integer.valueOf(55), paramInt, mavContainer, null);

		assertEquals("Didn't use the 1st registered handler", Integer.valueOf(55), h1.getReturnValue());
		assertNull("Shouldn't have use the 2nd registered handler", h2.getReturnValue());
	}

	@Test(expected=IllegalArgumentException.class)
	public void noSuitableReturnValueHandler() throws Exception {
		registerHandler(Integer.class);
		this.handlers.handleReturnValue("value", paramStr, null, null);
	}

	private StubReturnValueHandler registerHandler(Class<?> returnType) {
		StubReturnValueHandler handler = new StubReturnValueHandler(returnType);
		handlers.addHandler(handler);
		return handler;
	}

	@SuppressWarnings("unused")
	private Integer handleInteger() {
		return null;
	}

	@SuppressWarnings("unused")
	private String handleString() {
		return null;
	}

}