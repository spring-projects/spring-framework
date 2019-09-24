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

package org.springframework.core.task;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link CompositeTaskDecorator}.
 *
 * @author Tadaya Tsuyukubo
 */
class CompositeTaskDecoratorTests {

	@Test
	void decorate() {
		List<String> decorated = new ArrayList<>();
		CompositeTaskDecorator compositeTaskDecorator = new CompositeTaskDecorator(runnable -> {
			decorated.add("foo");
			return runnable;
		});
		compositeTaskDecorator.add(runnable -> {
			decorated.add("bar");
			return runnable;
		});

		Runnable runnable = compositeTaskDecorator.decorate(() -> {
			decorated.add("baz");
		});

		runnable.run();

		assertThat(decorated).containsExactly("foo", "bar", "baz");
	}
}
