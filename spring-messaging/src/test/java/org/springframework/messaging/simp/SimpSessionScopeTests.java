/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SimpSessionScope}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
class SimpSessionScopeTests {

	private SimpSessionScope scope = new SimpSessionScope();

	@SuppressWarnings("rawtypes")
	private ObjectFactory objectFactory = mock();

	private SimpAttributes simpAttributes = new SimpAttributes("session1", new ConcurrentHashMap<>());


	@BeforeEach
	void setUp() {
		SimpAttributesContextHolder.setAttributes(this.simpAttributes);
	}

	@AfterEach
	void tearDown() {
		SimpAttributesContextHolder.resetAttributes();
	}

	@Test
	void get() {
		this.simpAttributes.setAttribute("name", "value");
		Object actual = this.scope.get("name", this.objectFactory);

		assertThat(actual).isEqualTo("value");
	}

	@Test
	void getWithObjectFactory() {
		given(this.objectFactory.getObject()).willReturn("value");
		Object actual = this.scope.get("name", this.objectFactory);

		assertThat(actual).isEqualTo("value");
		assertThat(this.simpAttributes.getAttribute("name")).isEqualTo("value");
	}

	@Test
	void remove() {
		this.simpAttributes.setAttribute("name", "value");

		Object removed = this.scope.remove("name");
		assertThat(removed).isEqualTo("value");
		assertThat(this.simpAttributes.getAttribute("name")).isNull();

		removed = this.scope.remove("name");
		assertThat(removed).isNull();
	}

	@Test
	void registerDestructionCallback() {
		Runnable runnable = mock();
		this.scope.registerDestructionCallback("name", runnable);

		this.simpAttributes.sessionCompleted();
		verify(runnable, times(1)).run();
	}

	@Test
	void getSessionId() {
		assertThat(this.scope.getConversationId()).isEqualTo("session1");
	}

}
