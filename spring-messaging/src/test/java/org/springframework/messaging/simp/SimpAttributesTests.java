/*
 * Copyright 2002-2018 the original author or authors.
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for
 * {@link org.springframework.messaging.simp.SimpAttributes}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class SimpAttributesTests {

	private SimpAttributes simpAttributes;

	private Map<String, Object> map;

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Before
	public void setup() {
		this.map = new ConcurrentHashMap<>();
		this.simpAttributes = new SimpAttributes("session1", this.map);
	}


	@Test
	public void getAttribute() {
		this.simpAttributes.setAttribute("name1", "value1");

		assertThat(this.simpAttributes.getAttribute("name1"), is("value1"));
		assertThat(this.simpAttributes.getAttribute("name2"), nullValue());
	}

	@Test
	public void getAttributeNames() {
		this.simpAttributes.setAttribute("name1", "value1");
		this.simpAttributes.setAttribute("name2", "value1");
		this.simpAttributes.setAttribute("name3", "value1");

		assertThat(this.simpAttributes.getAttributeNames(), arrayContainingInAnyOrder("name1", "name2", "name3"));
	}

	@Test
	public void registerDestructionCallback() {
		Runnable callback = Mockito.mock(Runnable.class);
		this.simpAttributes.registerDestructionCallback("name1", callback);

		assertThat(this.simpAttributes.getAttribute(
				SimpAttributes.DESTRUCTION_CALLBACK_NAME_PREFIX + "name1"), sameInstance(callback));
	}

	@Test
	public void registerDestructionCallbackAfterSessionCompleted() {
		this.simpAttributes.sessionCompleted();
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage(containsString("already completed"));
		this.simpAttributes.registerDestructionCallback("name1", Mockito.mock(Runnable.class));
	}

	@Test
	public void removeDestructionCallback() {
		Runnable callback1 = Mockito.mock(Runnable.class);
		Runnable callback2 = Mockito.mock(Runnable.class);
		this.simpAttributes.registerDestructionCallback("name1", callback1);
		this.simpAttributes.registerDestructionCallback("name2", callback2);

		assertThat(this.simpAttributes.getAttributeNames().length, is(2));
	}

	@Test
	public void getSessionMutex() {
		assertThat(this.simpAttributes.getSessionMutex(), sameInstance(this.map));
	}

	@Test
	public void getSessionMutexExplicit() {
		Object mutex = new Object();
		this.simpAttributes.setAttribute(SimpAttributes.SESSION_MUTEX_NAME, mutex);

		assertThat(this.simpAttributes.getSessionMutex(), sameInstance(mutex));
	}

	@Test
	public void sessionCompleted() {
		Runnable callback1 = Mockito.mock(Runnable.class);
		Runnable callback2 = Mockito.mock(Runnable.class);
		this.simpAttributes.registerDestructionCallback("name1", callback1);
		this.simpAttributes.registerDestructionCallback("name2", callback2);

		this.simpAttributes.sessionCompleted();

		verify(callback1, times(1)).run();
		verify(callback2, times(1)).run();
	}

	@Test
	public void sessionCompletedIsIdempotent() {
		Runnable callback1 = Mockito.mock(Runnable.class);
		this.simpAttributes.registerDestructionCallback("name1", callback1);

		this.simpAttributes.sessionCompleted();
		this.simpAttributes.sessionCompleted();
		this.simpAttributes.sessionCompleted();

		verify(callback1, times(1)).run();
	}

}
