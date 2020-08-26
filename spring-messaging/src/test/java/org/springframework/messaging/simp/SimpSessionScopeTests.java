/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.simp;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.ObjectFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link org.springframework.messaging.simp.SimpSessionScope}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class SimpSessionScopeTests {

	private SimpSessionScope scope;

	@SuppressWarnings("rawtypes")
	private ObjectFactory objectFactory;

	private SimpAttributes simpAttributes;


	@BeforeEach
	public void setUp() {
		this.scope = new SimpSessionScope();
		this.objectFactory = Mockito.mock(ObjectFactory.class);
		this.simpAttributes = new SimpAttributes("session1", new ConcurrentHashMap<>());
		SimpAttributesContextHolder.setAttributes(this.simpAttributes);
	}

	@AfterEach
	public void tearDown() {
		SimpAttributesContextHolder.resetAttributes();
	}

	@Test
	public void get() {
		this.simpAttributes.setAttribute("name", "value");
		Object actual = this.scope.get("name", this.objectFactory);

		assertThat(actual).isEqualTo("value");
	}

	@Test
	public void getWithObjectFactory() {
		given(this.objectFactory.getObject()).willReturn("value");
		Object actual = this.scope.get("name", this.objectFactory);

		assertThat(actual).isEqualTo("value");
		assertThat(this.simpAttributes.getAttribute("name")).isEqualTo("value");
	}

	@Test
	public void remove() {
		this.simpAttributes.setAttribute("name", "value");

		Object removed = this.scope.remove("name");
		assertThat(removed).isEqualTo("value");
		assertThat(this.simpAttributes.getAttribute("name")).isNull();

		removed = this.scope.remove("name");
		assertThat(removed).isNull();
	}

	@Test
	public void registerDestructionCallback() {
		Runnable runnable = Mockito.mock(Runnable.class);
		this.scope.registerDestructionCallback("name", runnable);

		this.simpAttributes.sessionCompleted();
		verify(runnable, times(1)).run();
	}

	@Test
	public void getSessionId() {
		assertThat(this.scope.getConversationId()).isEqualTo("session1");
	}


}
