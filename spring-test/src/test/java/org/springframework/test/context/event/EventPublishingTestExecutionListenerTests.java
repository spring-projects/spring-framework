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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.test.context.TestContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EventPublishingTestExecutionListener}.
 * 
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 5.2
 */
@RunWith(MockitoJUnitRunner.class)
public class EventPublishingTestExecutionListenerTests {

	private final EventPublishingTestExecutionListener listener = new EventPublishingTestExecutionListener();

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private TestContext testContext;

	@Captor
	private ArgumentCaptor<TestContextEvent> testExecutionEvent;

	@Rule
	public final TestName testName = new TestName();


	@Before
	public void configureMock() {
		if (testName.getMethodName().startsWith("publish")) {
			when(testContext.hasApplicationContext()).thenReturn(true);
		}
	}

	@Test
	public void publishBeforeTestClassEvent() {
		assertEvent(BeforeTestClassEvent.class, listener::beforeTestClass);
	}

	@Test
	public void publishPrepareTestInstanceEvent() {
		assertEvent(PrepareTestInstanceEvent.class, listener::prepareTestInstance);
	}

	@Test
	public void publishBeforeTestMethodEvent() {
		assertEvent(BeforeTestMethodEvent.class, listener::beforeTestMethod);
	}

	@Test
	public void publishBeforeTestExecutionEvent() {
		assertEvent(BeforeTestExecutionEvent.class, listener::beforeTestExecution);
	}

	@Test
	public void publishAfterTestExecutionEvent() {
		assertEvent(AfterTestExecutionEvent.class, listener::afterTestExecution);
	}

	@Test
	public void publishAfterTestMethodEvent() {
		assertEvent(AfterTestMethodEvent.class, listener::afterTestMethod);
	}

	@Test
	public void publishAfterTestClassEvent() {
		assertEvent(AfterTestClassEvent.class, listener::afterTestClass);
	}

	private void assertEvent(Class<? extends TestContextEvent> eventClass, Consumer<TestContext> callback) {
		callback.accept(testContext);
		verify(testContext.getApplicationContext(), only()).publishEvent(testExecutionEvent.capture());
		assertThat(testExecutionEvent.getValue(), instanceOf(eventClass));
		assertThat(testExecutionEvent.getValue().getSource(), equalTo(testContext));
	}

	@Test
	public void doesNotPublishBeforeTestClassEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(listener::beforeTestClass);
	}

	@Test
	public void doesNotPublishPrepareTestInstanceEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(listener::prepareTestInstance);
	}

	@Test
	public void doesNotPublishBeforeTestMethodEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(listener::beforeTestMethod);
	}

	@Test
	public void doesNotPublishBeforeTestExecutionEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(listener::beforeTestExecution);
	}

	@Test
	public void doesNotPublishAfterTestExecutionEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(listener::afterTestExecution);
	}

	@Test
	public void doesNotPublishAfterTestMethodEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(listener::afterTestMethod);
	}

	@Test
	public void doesNotPublishAfterTestClassEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(listener::afterTestClass);
	}

	private void assertNoEvent(Consumer<TestContext> callback) {
		callback.accept(testContext);
		verify(testContext.getApplicationContext(), never()).publishEvent(any());
	}

}
