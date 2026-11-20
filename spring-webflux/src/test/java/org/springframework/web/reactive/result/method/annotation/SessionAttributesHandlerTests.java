/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;


import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.server.WebSession;
import org.springframework.web.testfixture.server.MockWebSession;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SessionAttributesHandler}.
 *
 * @author Rossen Stoyanchev
 */
class SessionAttributesHandlerTests {

	private final SessionAttributesHandler sessionAttributesHandler =
			new SessionAttributesHandler(TestController.class);


	@Test
	void isSessionAttribute() {
		assertThat(this.sessionAttributesHandler.isHandlerSessionAttribute("attr1", String.class)).isTrue();
		assertThat(this.sessionAttributesHandler.isHandlerSessionAttribute("attr2", String.class)).isTrue();
		assertThat(this.sessionAttributesHandler.isHandlerSessionAttribute("simple", TestBean.class)).isTrue();
		assertThat(this.sessionAttributesHandler.isHandlerSessionAttribute("simple", String.class)).isFalse();
	}

	@Test
	void retrieveAttributes() {
		WebSession session = new MockWebSession();
		session.getAttributes().put("attr1", "value1");
		session.getAttributes().put("attr2", "value2");
		session.getAttributes().put("attr3", new TestBean());
		session.getAttributes().put("attr4", new TestBean());

		assertThat(sessionAttributesHandler.retrieveAttributes(session).keySet()).as("Named attributes (attr1, attr2) should be 'known' right away").isEqualTo(new HashSet<>(asList("attr1", "attr2")));

		// Resolve 'attr3' by type
		sessionAttributesHandler.isHandlerSessionAttribute("attr3", TestBean.class);

		assertThat(sessionAttributesHandler.retrieveAttributes(session).keySet()).as("Named attributes (attr1, attr2) and resolved attribute (att3) should be 'known'").isEqualTo(new HashSet<>(asList("attr1", "attr2", "attr3")));
	}

	@Test
	void cleanupAttributes() {
		WebSession session = new MockWebSession();
		session.getAttributes().put("attr1", "value1");
		session.getAttributes().put("attr2", "value2");
		session.getAttributes().put("attr3", new TestBean());

		this.sessionAttributesHandler.cleanupAttributes(session);

		assertThat(session.getAttributes().get("attr1")).isNull();
		assertThat(session.getAttributes().get("attr2")).isNull();
		assertThat(session.getAttributes().get("attr3")).isNotNull();

		// Resolve 'attr3' by type
		this.sessionAttributesHandler.isHandlerSessionAttribute("attr3", TestBean.class);
		this.sessionAttributesHandler.cleanupAttributes(session);

		assertThat(session.getAttributes().get("attr3")).isNull();
	}

	@Test
	void storeAttributes() {
		Map<String, Object> model = Map.of(
				"attr1", "value1",
				"attr2", "value2",
				"attr3", new TestBean()
			);

		WebSession session = new MockWebSession();
		sessionAttributesHandler.storeAttributes(session, model);

		assertThat(session.getAttributes().get("attr1")).isEqualTo("value1");
		assertThat(session.getAttributes().get("attr2")).isEqualTo("value2");
		assertThat(session.getAttributes().get("attr3")).isInstanceOf(TestBean.class);
	}


	@SessionAttributes(names = { "attr1", "attr2" }, types = { TestBean.class })
	private static class TestController {
	}

}
