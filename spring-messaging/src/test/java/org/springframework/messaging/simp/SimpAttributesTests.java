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

package org.springframework.messaging.simp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SimpAttributes}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
class SimpAttributesTests {

	private final Map<String, Object> map = new ConcurrentHashMap<>();

	private final SimpAttributes simpAttributes = new SimpAttributes("session1", this.map);


	@Test
	void getAttribute() {
		this.simpAttributes.setAttribute("name1", "value1");

		assertThat(this.simpAttributes.getAttribute("name1")).isEqualTo("value1");
		assertThat(this.simpAttributes.getAttribute("name2")).isNull();
	}

	@Test
	void getAttributeNames() {
		this.simpAttributes.setAttribute("name1", "value1");
		this.simpAttributes.setAttribute("name2", "value1");
		this.simpAttributes.setAttribute("name3", "value1");

		assertThat(this.simpAttributes.getAttributeNames())
				.containsExactlyInAnyOrder("name1", "name2", "name3");
	}

	@Test
	void registerDestructionCallback() {
		Runnable callback = mock();
		this.simpAttributes.registerDestructionCallback("name1", callback);

		assertThat(this.simpAttributes.getAttribute(
				SimpAttributes.DESTRUCTION_CALLBACK_NAME_PREFIX + "name1")).isSameAs(callback);
	}

	@Test
	void registerDestructionCallbackAfterSessionCompleted() {
		this.simpAttributes.sessionCompleted();
		assertThatIllegalStateException()
				.isThrownBy(() -> this.simpAttributes.registerDestructionCallback("name1", mock()))
				.withMessageContaining("already completed");
	}

	@Test
	void removeDestructionCallback() {
		Runnable callback1 = mock();
		Runnable callback2 = mock();
		this.simpAttributes.registerDestructionCallback("name1", callback1);
		this.simpAttributes.registerDestructionCallback("name2", callback2);

		assertThat(this.simpAttributes.getAttributeNames()).hasSize(2);
	}

	@Test
	void getSessionMutex() {
		assertThat(this.simpAttributes.getSessionMutex()).isSameAs(this.map);
	}

	@Test
	void getSessionMutexExplicit() {
		Object mutex = new Object();
		this.simpAttributes.setAttribute(SimpAttributes.SESSION_MUTEX_NAME, mutex);

		assertThat(this.simpAttributes.getSessionMutex()).isSameAs(mutex);
	}

	@Test
	void sessionCompleted() {
		Runnable callback1 = mock();
		Runnable callback2 = mock();
		this.simpAttributes.registerDestructionCallback("name1", callback1);
		this.simpAttributes.registerDestructionCallback("name2", callback2);

		this.simpAttributes.sessionCompleted();

		verify(callback1, times(1)).run();
		verify(callback2, times(1)).run();
	}

	@Test
	void sessionCompletedIsIdempotent() {
		Runnable callback1 = mock();
		this.simpAttributes.registerDestructionCallback("name1", callback1);

		this.simpAttributes.sessionCompleted();
		this.simpAttributes.sessionCompleted();
		this.simpAttributes.sessionCompleted();

		verify(callback1, times(1)).run();
	}

}
