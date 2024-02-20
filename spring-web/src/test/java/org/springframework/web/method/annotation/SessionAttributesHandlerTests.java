/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link SessionAttributesHandler}.
 *
 * @author Rossen Stoyanchev
 */
class SessionAttributesHandlerTests {

	private final SessionAttributeStore sessionAttributeStore = new DefaultSessionAttributeStore();

	private final SessionAttributesHandler sessionAttributesHandler =
			new SessionAttributesHandler(TestSessionAttributesHolder.class, sessionAttributeStore);

	private final NativeWebRequest request = new ServletWebRequest(new MockHttpServletRequest());


	@Test
	void isSessionAttribute() {
		assertThat(sessionAttributesHandler.isHandlerSessionAttribute("attr1", String.class)).isTrue();
		assertThat(sessionAttributesHandler.isHandlerSessionAttribute("attr2", String.class)).isTrue();
		assertThat(sessionAttributesHandler.isHandlerSessionAttribute("simple", TestBean.class)).isTrue();
		assertThat(sessionAttributesHandler.isHandlerSessionAttribute("simple", String.class)).isFalse();
	}

	@Test
	void retrieveAttributes() {
		sessionAttributeStore.storeAttribute(request, "attr1", "value1");
		sessionAttributeStore.storeAttribute(request, "attr2", "value2");
		sessionAttributeStore.storeAttribute(request, "attr3", new TestBean());
		sessionAttributeStore.storeAttribute(request, "attr4", new TestBean());

		assertThat(sessionAttributesHandler.retrieveAttributes(request).keySet())
				.as("Named attributes (attr1, attr2) should be 'known' right away")
				.isEqualTo(new HashSet<>(asList("attr1", "attr2")));

		// Resolve 'attr3' by type
		sessionAttributesHandler.isHandlerSessionAttribute("attr3", TestBean.class);

		assertThat(sessionAttributesHandler.retrieveAttributes(request).keySet())
				.as("Named attributes (attr1, attr2) and resolved attribute (attr3) should be 'known'")
				.isEqualTo(new HashSet<>(asList("attr1", "attr2", "attr3")));
	}

	@Test
	void cleanupAttributes() {
		sessionAttributeStore.storeAttribute(request, "attr1", "value1");
		sessionAttributeStore.storeAttribute(request, "attr2", "value2");
		sessionAttributeStore.storeAttribute(request, "attr3", new TestBean());

		sessionAttributesHandler.cleanupAttributes(request);

		assertThat(sessionAttributeStore.retrieveAttribute(request, "attr1")).isNull();
		assertThat(sessionAttributeStore.retrieveAttribute(request, "attr2")).isNull();
		assertThat(sessionAttributeStore.retrieveAttribute(request, "attr3")).isNotNull();

		// Resolve 'attr3' by type
		sessionAttributesHandler.isHandlerSessionAttribute("attr3", TestBean.class);
		sessionAttributesHandler.cleanupAttributes(request);

		assertThat(sessionAttributeStore.retrieveAttribute(request, "attr3")).isNull();
	}

	@Test
	void storeAttributes() {
		ModelMap model = new ModelMap();
		model.put("attr1", "value1");
		model.put("attr2", "value2");
		model.put("attr3", new TestBean());

		sessionAttributesHandler.storeAttributes(request, model);

		assertThat(sessionAttributeStore.retrieveAttribute(request, "attr1")).isEqualTo("value1");
		assertThat(sessionAttributeStore.retrieveAttribute(request, "attr2")).isEqualTo("value2");
		boolean condition = sessionAttributeStore.retrieveAttribute(request, "attr3") instanceof TestBean;
		assertThat(condition).isTrue();
	}


	@SessionAttributes(names = {"attr1", "attr2"}, types = TestBean.class)
	private static class TestSessionAttributesHolder {
	}

}
