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

import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.TestBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Test fixture with {@link SessionAttributesHandler}.
 * 
 * @author Rossen Stoyanchev
 */
public class SessionAttributesHandlerTests {

	private Class<?> handlerType = SessionAttributeHandler.class;
	
	private SessionAttributesHandler sessionAttributesHandler;
	
	private SessionAttributeStore sessionAttributeStore;
	
	private NativeWebRequest request;

	@Before
	public void setUp() {
		this.sessionAttributeStore = new DefaultSessionAttributeStore();
		this.sessionAttributesHandler = new SessionAttributesHandler(handlerType, sessionAttributeStore);
		this.request = new ServletWebRequest(new MockHttpServletRequest());
	}
	
	@Test
	public void isSessionAttribute() throws Exception {
		assertTrue(sessionAttributesHandler.isHandlerSessionAttribute("attr1", null));
		assertTrue(sessionAttributesHandler.isHandlerSessionAttribute("attr2", null));
		assertTrue(sessionAttributesHandler.isHandlerSessionAttribute("simple", TestBean.class));
		assertFalse(sessionAttributesHandler.isHandlerSessionAttribute("simple", null));
	}

	@Test
	public void retrieveAttributes() throws Exception {
		sessionAttributeStore.storeAttribute(request, "attr1", "value1");
		sessionAttributeStore.storeAttribute(request, "attr2", "value2");
		sessionAttributeStore.storeAttribute(request, "attr3", new TestBean());

		// Resolve successfully handler session attributes once
		assertTrue(sessionAttributesHandler.isHandlerSessionAttribute("attr1", null));
		assertTrue(sessionAttributesHandler.isHandlerSessionAttribute("attr3", TestBean.class));

		Map<String, ?> attributes = sessionAttributesHandler.retrieveAttributes(request);

		assertEquals(new HashSet<String>(asList("attr1", "attr3")), attributes.keySet());
	}

	@Test
	public void cleanupAttributes() throws Exception {
		sessionAttributeStore.storeAttribute(request, "attr1", "value1");
		sessionAttributeStore.storeAttribute(request, "attr2", "value2");
		sessionAttributeStore.storeAttribute(request, "attr3", new TestBean());

		// Resolve successfully handler session attributes once
		assertTrue(sessionAttributesHandler.isHandlerSessionAttribute("attr1", null));
		assertTrue(sessionAttributesHandler.isHandlerSessionAttribute("attr3", TestBean.class));

		sessionAttributesHandler.cleanupAttributes(request);
		
		assertNull(sessionAttributeStore.retrieveAttribute(request, "attr1"));
		assertNotNull(sessionAttributeStore.retrieveAttribute(request, "attr2"));
		assertNull(sessionAttributeStore.retrieveAttribute(request, "attr3"));
	}

	@Test
	public void storeAttributes() throws Exception {
		ModelMap model = new ModelMap();
		model.put("attr1", "value1");
		model.put("attr2", "value2");
		model.put("attr3", new TestBean());
		
		// Resolve successfully handler session attributes once
		assertTrue(sessionAttributesHandler.isHandlerSessionAttribute("attr1", null));
		assertTrue(sessionAttributesHandler.isHandlerSessionAttribute("attr2", null));
		assertTrue(sessionAttributesHandler.isHandlerSessionAttribute("attr3", TestBean.class));
		
		sessionAttributesHandler.storeAttributes(request, model);
		
		assertEquals("value1", sessionAttributeStore.retrieveAttribute(request, "attr1"));
		assertEquals("value2", sessionAttributeStore.retrieveAttribute(request, "attr2"));
		assertTrue(sessionAttributeStore.retrieveAttribute(request, "attr3") instanceof TestBean);
	}
	
	@SessionAttributes(value = { "attr1", "attr2" }, types = { TestBean.class })
	private static class SessionAttributeHandler {
	}

}