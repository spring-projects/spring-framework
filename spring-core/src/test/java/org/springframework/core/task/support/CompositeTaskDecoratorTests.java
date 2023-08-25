/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.task.support;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.core.task.TaskDecorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CompositeTaskDecorator}.
 *
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 */
class CompositeTaskDecoratorTests {

	@Test
	void createWithNullCollection() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CompositeTaskDecorator(null))
				.withMessage("TaskDecorators must not be null");
	}

	@Test
	void decorateWithNullRunnable() {
		CompositeTaskDecorator taskDecorator = new CompositeTaskDecorator(List.of());
		assertThatIllegalArgumentException().isThrownBy(() -> taskDecorator.decorate(null))
				.withMessage("Runnable must not be null");
	}

	@Test
	void decorate() {
		TaskDecorator first = mockNoOpTaskDecorator();
		TaskDecorator second = mockNoOpTaskDecorator();
		TaskDecorator third = mockNoOpTaskDecorator();
		CompositeTaskDecorator taskDecorator = new CompositeTaskDecorator(List.of(first, second, third));
		Runnable runnable = mock();
		taskDecorator.decorate(runnable);
		InOrder ordered = inOrder(first, second, third);
		ordered.verify(first).decorate(runnable);
		ordered.verify(second).decorate(runnable);
		ordered.verify(third).decorate(runnable);
	}

	@Test
	void decorateReusesResultOfPreviousRun() {
		Runnable original = mock();
		Runnable firstDecorated = mock();
		TaskDecorator first = mock();
		given(first.decorate(original)).willReturn(firstDecorated);
		Runnable secondDecorated = mock();
		TaskDecorator second = mock();
		given(second.decorate(firstDecorated)).willReturn(secondDecorated);
		Runnable result = new CompositeTaskDecorator(List.of(first, second)).decorate(original);
		assertThat(result).isSameAs(secondDecorated);
		verify(first).decorate(original);
		verify(second).decorate(firstDecorated);
	}

	private TaskDecorator mockNoOpTaskDecorator() {
		TaskDecorator mock = mock();
		given(mock.decorate(any())).willAnswer(invocation -> invocation.getArguments()[0]);
		return mock;
	}

}
