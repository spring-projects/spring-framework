/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;


import java.time.Duration;
import java.util.HashSet;

import org.junit.Test;

import org.springframework.tests.sample.beans.TestBean;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.InMemoryWebSessionStore;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test fixture with {@link SessionAttributesHandler}.
 * @author Rossen Stoyanchev
 */
public class SessionAttributesHandlerTests {

	private final SessionAttributesHandler sessionAttributesHandler =
			new SessionAttributesHandler(TestController.class);


	@Test
	public void isSessionAttribute() throws Exception {
		assertTrue(this.sessionAttributesHandler.isHandlerSessionAttribute("attr1", String.class));
		assertTrue(this.sessionAttributesHandler.isHandlerSessionAttribute("attr2", String.class));
		assertTrue(this.sessionAttributesHandler.isHandlerSessionAttribute("simple", TestBean.class));
		assertFalse(this.sessionAttributesHandler.isHandlerSessionAttribute("simple", String.class));
	}

	@Test
	public void retrieveAttributes() throws Exception {
		WebSession session = new InMemoryWebSessionStore().createWebSession().block(Duration.ZERO);
		assertNotNull(session);

		session.getAttributes().put("attr1", "value1");
		session.getAttributes().put("attr2", "value2");
		session.getAttributes().put("attr3", new TestBean());
		session.getAttributes().put("attr4", new TestBean());

		assertEquals("Named attributes (attr1, attr2) should be 'known' right away",
				new HashSet<>(asList("attr1", "attr2")),
				sessionAttributesHandler.retrieveAttributes(session).keySet());

		// Resolve 'attr3' by type
		sessionAttributesHandler.isHandlerSessionAttribute("attr3", TestBean.class);

		assertEquals("Named attributes (attr1, attr2) and resolved attribute (att3) should be 'known'",
				new HashSet<>(asList("attr1", "attr2", "attr3")),
				sessionAttributesHandler.retrieveAttributes(session).keySet());
	}

	@Test
	public void cleanupAttributes() throws Exception {
		WebSession session = new InMemoryWebSessionStore().createWebSession().block(Duration.ZERO);
		assertNotNull(session);

		session.getAttributes().put("attr1", "value1");
		session.getAttributes().put("attr2", "value2");
		session.getAttributes().put("attr3", new TestBean());

		this.sessionAttributesHandler.cleanupAttributes(session);

		assertNull(session.getAttributes().get("attr1"));
		assertNull(session.getAttributes().get("attr2"));
		assertNotNull(session.getAttributes().get("attr3"));

		// Resolve 'attr3' by type
		this.sessionAttributesHandler.isHandlerSessionAttribute("attr3", TestBean.class);
		this.sessionAttributesHandler.cleanupAttributes(session);

		assertNull(session.getAttributes().get("attr3"));
	}

	@Test
	public void storeAttributes() throws Exception {
		WebSession session = new InMemoryWebSessionStore().createWebSession().block(Duration.ZERO);
		assertNotNull(session);

		ModelMap model = new ModelMap();
		model.put("attr1", "value1");
		model.put("attr2", "value2");
		model.put("attr3", new TestBean());

		sessionAttributesHandler.storeAttributes(session, model);

		assertEquals("value1", session.getAttributes().get("attr1"));
		assertEquals("value2", session.getAttributes().get("attr2"));
		assertTrue(session.getAttributes().get("attr3") instanceof TestBean);
	}


	@SessionAttributes(names = { "attr1", "attr2" }, types = { TestBean.class })
	private static class TestController {
	}

}