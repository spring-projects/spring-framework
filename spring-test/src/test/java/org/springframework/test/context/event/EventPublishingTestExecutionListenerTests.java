/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.event;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.test.context.TestContext;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link EventPublishingTestExecutionListener}.
 * 
 * @author Frank Scheffler
 * @since 5.2
 */
@RunWith(MockitoJUnitRunner.class)
public class EventPublishingTestExecutionListenerTests {

	private final EventPublishingTestExecutionListener listener = new EventPublishingTestExecutionListener();

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private TestContext testContext;

	@Captor
	private ArgumentCaptor<TestContextEvent> testExecutionEvent;


	@Test
	public void publishBeforeTestClassEvent() {
		listener.beforeTestClass(testContext);
		assertEvent(BeforeTestClassEvent.class);
	}

	@Test
	public void publishPrepareTestInstanceEvent() {
		listener.prepareTestInstance(testContext);
		assertEvent(PrepareTestInstanceEvent.class);
	}

	@Test
	public void publishBeforeTestMethodEvent() {
		listener.beforeTestMethod(testContext);
		assertEvent(BeforeTestMethodEvent.class);
	}

	@Test
	public void publishBeforeTestExecutionEvent() {
		listener.beforeTestExecution(testContext);
		assertEvent(BeforeTestExecutionEvent.class);
	}

	@Test
	public void publishAfterTestExecutionEvent() {
		listener.afterTestExecution(testContext);
		assertEvent(AfterTestExecutionEvent.class);
	}

	@Test
	public void publishAfterTestMethodEvent() {
		listener.afterTestMethod(testContext);
		assertEvent(AfterTestMethodEvent.class);
	}

	@Test
	public void publishAfterTestClassEvent() {
		listener.afterTestClass(testContext);
		assertEvent(AfterTestClassEvent.class);
	}

	private void assertEvent(Class<? extends TestContextEvent> eventClass) {
		verify(testContext.getApplicationContext(), only()).publishEvent(testExecutionEvent.capture());
		assertThat(testExecutionEvent.getValue(), instanceOf(eventClass));
		assertThat(testExecutionEvent.getValue().getSource(), equalTo(testContext));
	}

}
