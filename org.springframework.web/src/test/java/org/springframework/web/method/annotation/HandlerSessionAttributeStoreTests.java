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

package org.springframework.web.method.annotation;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.TestBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.SessionAttributesHandler;

/**
 * Test fixture for {@link SessionAttributesHandler} unit tests.
 * 
 * @author Rossen Stoyanchev
 */
public class HandlerSessionAttributeStoreTests {

	private DefaultSessionAttributeStore sessionAttributes;
	
	private SessionAttributesHandler handlerSessionAttributes;
	
	private Class<?> handlerType = SessionAttributeHandler.class;
	
	private NativeWebRequest request;

	@Before
	public void setUp() {
		this.sessionAttributes = new DefaultSessionAttributeStore();
		this.handlerSessionAttributes = new SessionAttributesHandler(handlerType, sessionAttributes);

		this.request = new ServletWebRequest(new MockHttpServletRequest());
	}
	
	@Test
	public void isSessionAttribute() throws Exception {
		assertTrue(handlerSessionAttributes.isHandlerSessionAttribute("attr1", null));
		assertTrue(handlerSessionAttributes.isHandlerSessionAttribute("attr2", null));
		assertTrue(handlerSessionAttributes.isHandlerSessionAttribute("simple", TestBean.class));
		
		assertFalse("Attribute name not known", handlerSessionAttributes.isHandlerSessionAttribute("simple", null));
	}

	@Test
	public void retrieveAttributes() throws Exception {
		sessionAttributes.storeAttribute(request, "attr1", "value1");
		sessionAttributes.storeAttribute(request, "attr2", "value2");
		sessionAttributes.storeAttribute(request, "attr3", new TestBean());

		// Query attributes to associate them with the handler type 
		assertTrue(handlerSessionAttributes.isHandlerSessionAttribute("attr1", null));
		assertTrue(handlerSessionAttributes.isHandlerSessionAttribute("attr3", TestBean.class));

		Map<String, ?> attributes = handlerSessionAttributes.retrieveHandlerSessionAttributes(request);

		assertEquals(new HashSet<String>(asList("attr1", "attr3")), attributes.keySet());
	}

	@Test
	public void cleanupAttribute() throws Exception {
		sessionAttributes.storeAttribute(request, "attr1", "value1");
		sessionAttributes.storeAttribute(request, "attr2", "value2");
		sessionAttributes.storeAttribute(request, "attr3", new TestBean());

		// Query attribute to associate it with the handler type 
		assertTrue(handlerSessionAttributes.isHandlerSessionAttribute("attr1", null));
		assertTrue(handlerSessionAttributes.isHandlerSessionAttribute("attr3", TestBean.class));

		handlerSessionAttributes.cleanupHandlerSessionAttributes(request);
		
		assertNull(sessionAttributes.retrieveAttribute(request, "attr1"));
		assertNotNull(sessionAttributes.retrieveAttribute(request, "attr2"));
		assertNull(sessionAttributes.retrieveAttribute(request, "attr3"));
	}

	@Test
	public void storeAttributes() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("attr1", "value1");
		attributes.put("attr2", "value2");
		attributes.put("attr3", new TestBean());
		
		// Query attribute to associate it with the handler type 
		assertTrue(handlerSessionAttributes.isHandlerSessionAttribute("attr1", null));
		assertTrue(handlerSessionAttributes.isHandlerSessionAttribute("attr2", null));
		assertTrue(handlerSessionAttributes.isHandlerSessionAttribute("attr3", TestBean.class));
		
		handlerSessionAttributes.storeHandlerSessionAttributes(request, attributes);
		
		assertEquals("value1", sessionAttributes.retrieveAttribute(request, "attr1"));
		assertEquals("value2", sessionAttributes.retrieveAttribute(request, "attr2"));
		assertTrue(sessionAttributes.retrieveAttribute(request, "attr3") instanceof TestBean);
	}
	
	@SessionAttributes(value = { "attr1", "attr2" }, types = { TestBean.class })
	private static class SessionAttributeHandler {
	}
}
