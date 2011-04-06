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
 * Test fixture for {@link HandlerMethodReturnValueHandlerContainer} unit tests.
 * 
 * @author Rossen Stoyanchev
 */
public class HandlerMethodReturnValueHandlerContainerTests {

	private HandlerMethodReturnValueHandlerContainer container;

	ModelAndViewContainer<?> mavContainer;
	
	private MethodParameter paramInteger;

	private MethodParameter paramString;

	@Before
	public void setUp() throws Exception {
		this.container = new HandlerMethodReturnValueHandlerContainer();

		this.paramInteger = new MethodParameter(getClass().getDeclaredMethod("handleInteger"), -1);
		this.paramString = new MethodParameter(getClass().getDeclaredMethod("handleString"), -1);
		
		mavContainer = new ModelAndViewContainer<Object>(null); 
	}
	
	@Test
	public void supportsReturnType() throws Exception {
		registerReturnValueHandler(Integer.class, false);
		
		assertTrue(this.container.supportsReturnType(paramInteger));
		assertFalse(this.container.supportsReturnType(paramString));
	}
	
	@Test
	public void handleReturnValue() throws Exception {
		StubReturnValueHandler handler = registerReturnValueHandler(Integer.class, false);
		this.container.handleReturnValue(Integer.valueOf(55), paramInteger, mavContainer, null);
		
		assertEquals(Integer.valueOf(55), handler.getUnhandledReturnValue());
	}

	@Test
	public void handleReturnValueMultipleHandlers() throws Exception {
		StubReturnValueHandler handler1 = registerReturnValueHandler(Integer.class, false);
		StubReturnValueHandler handler2 = registerReturnValueHandler(Integer.class, false);
		this.container.handleReturnValue(Integer.valueOf(55), paramInteger, mavContainer, null);
		
		assertEquals("Didn't use the 1st registered handler", Integer.valueOf(55), handler1.getUnhandledReturnValue());
		assertNull("Shouldn't have use the 2nd registered handler", handler2.getUnhandledReturnValue());
	}
	
	@Test(expected=IllegalStateException.class)
	public void noSuitableReturnValueHandler() throws Exception {
		registerReturnValueHandler(Integer.class, false);
		this.container.handleReturnValue("value", paramString, null, null);
	}

	@Test
	public void returnValueHandlerUsesResponse() throws Exception {
		registerReturnValueHandler(Integer.class, true);
		assertTrue(this.container.usesResponseArgument(paramInteger));
	}

	@Test
	public void returnValueHandlerDosntUseResponse() throws Exception {
		registerReturnValueHandler(Integer.class, false);
		assertFalse(this.container.usesResponseArgument(paramInteger));
	}
	
	protected StubReturnValueHandler registerReturnValueHandler(Class<?> returnType, boolean usesResponse) {
		StubReturnValueHandler handler = new StubReturnValueHandler(returnType, usesResponse);
		this.container.registerReturnValueHandler(handler);
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
