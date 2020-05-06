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

package org.springframework.test.context.event;

import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.test.context.TestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link EventPublishingTestExecutionListener}.
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 5.2
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class EventPublishingTestExecutionListenerTests {

	private final EventPublishingTestExecutionListener listener = new EventPublishingTestExecutionListener();

	@Mock
	private TestContext testContext;

	@Mock
	private ApplicationContext applicationContext;

	@Captor
	private ArgumentCaptor<Function<TestContext, ? extends ApplicationEvent>> eventFactory;


	@BeforeEach
	void configureMock(TestInfo testInfo) {
		// Force Mockito to invoke the interface default method
		willCallRealMethod().given(testContext).publishEvent(any());
		given(testContext.getApplicationContext()).willReturn(applicationContext);
		// Only allow events to be published for test methods named "publish*".
		given(testContext.hasApplicationContext()).willReturn(testInfo.getTestMethod().get().getName().startsWith("publish"));
	}

	@Test
	void publishBeforeTestClassEvent() {
		assertEvent(BeforeTestClassEvent.class, listener::beforeTestClass);
	}

	@Test
	void publishPrepareTestInstanceEvent() {
		assertEvent(PrepareTestInstanceEvent.class, listener::prepareTestInstance);
	}

	@Test
	void publishBeforeTestMethodEvent() {
		assertEvent(BeforeTestMethodEvent.class, listener::beforeTestMethod);
	}

	@Test
	void publishBeforeTestExecutionEvent() {
		assertEvent(BeforeTestExecutionEvent.class, listener::beforeTestExecution);
	}

	@Test
	void publishAfterTestExecutionEvent() {
		assertEvent(AfterTestExecutionEvent.class, listener::afterTestExecution);
	}

	@Test
	void publishAfterTestMethodEvent() {
		assertEvent(AfterTestMethodEvent.class, listener::afterTestMethod);
	}

	@Test
	void publishAfterTestClassEvent() {
		assertEvent(AfterTestClassEvent.class, listener::afterTestClass);
	}

	@Test
	void doesNotPublishBeforeTestClassEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(BeforeTestClassEvent.class, listener::beforeTestClass);
	}

	@Test
	void doesNotPublishPrepareTestInstanceEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(PrepareTestInstanceEvent.class, listener::prepareTestInstance);
	}

	@Test
	void doesNotPublishBeforeTestMethodEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(BeforeTestMethodEvent.class, listener::beforeTestMethod);
	}

	@Test
	void doesNotPublishBeforeTestExecutionEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(BeforeTestExecutionEvent.class, listener::beforeTestExecution);
	}

	@Test
	void doesNotPublishAfterTestExecutionEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(AfterTestExecutionEvent.class, listener::afterTestExecution);
	}

	@Test
	void doesNotPublishAfterTestMethodEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(AfterTestMethodEvent.class, listener::afterTestMethod);
	}

	@Test
	void doesNotPublishAfterTestClassEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(AfterTestClassEvent.class, listener::afterTestClass);
	}

	private void assertEvent(Class<? extends TestContextEvent> eventClass, Consumer<TestContext> callback) {
		callback.accept(testContext);

		// The listener attempted to publish the event...
		verify(testContext, times(1)).publishEvent(eventFactory.capture());

		// The listener successfully published the event...
		verify(applicationContext, times(1)).publishEvent(any());

		// Verify the type of event that was published.
		ApplicationEvent event = eventFactory.getValue().apply(testContext);
		assertThat(event).isInstanceOf(eventClass);
		assertThat(event.getSource()).isEqualTo(testContext);
	}

	private void assertNoEvent(Class<? extends TestContextEvent> eventClass, Consumer<TestContext> callback) {
		callback.accept(testContext);

		// The listener attempted to publish the event...
		verify(testContext, times(1)).publishEvent(eventFactory.capture());

		// But the event was not actually published since the ApplicationContext
		// was not available.
		verify(applicationContext, never()).publishEvent(any());

		// In any case, we can still verify the type of event that would have
		// been published.
		ApplicationEvent event = eventFactory.getValue().apply(testContext);
		assertThat(event).isInstanceOf(eventClass);
		assertThat(event.getSource()).isEqualTo(testContext);
	}

}
