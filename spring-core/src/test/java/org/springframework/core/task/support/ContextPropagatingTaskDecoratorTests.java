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

import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.context.ThreadLocalAccessor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContextPropagatingTaskDecorator}.
 * @author Brian Clozel
 */
class ContextPropagatingTaskDecoratorTests {

	@Test
	void shouldPropagateContextInTaskExecution() throws Exception {
		AtomicReference<String> actual = new AtomicReference<>("");
		ContextRegistry registry = new ContextRegistry();
		registry.registerThreadLocalAccessor(new TestThreadLocalAccessor());
		ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder().contextRegistry(registry).build();

		Runnable task = () -> actual.set(TestThreadLocalHolder.getValue());
		TestThreadLocalHolder.setValue("expected");

		Thread execution = new Thread(new ContextPropagatingTaskDecorator(snapshotFactory).decorate(task));
		execution.start();
		execution.join();
		assertThat(actual.get()).isEqualTo("expected");
		TestThreadLocalHolder.reset();
	}

	static class TestThreadLocalHolder {

		private static final ThreadLocal<String> holder = new ThreadLocal<>();

		static void setValue(String value) {
			holder.set(value);
		}

		static String getValue() {
			return holder.get();
		}

		static void reset() {
			holder.remove();
		}

	}

	static class TestThreadLocalAccessor implements ThreadLocalAccessor<String> {

		static final String KEY = "test.threadlocal";

		@Override
		public Object key() {
			return KEY;
		}

		@Override
		public String getValue() {
			return TestThreadLocalHolder.getValue();
		}

		@Override
		public void setValue(String value) {
			TestThreadLocalHolder.setValue(value);
		}

		@Override
		public void setValue() {
			TestThreadLocalHolder.reset();
		}

		@Override
		public void restore(String previousValue) {
			setValue(previousValue);
		}

	}

}
