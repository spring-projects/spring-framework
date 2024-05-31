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

package org.springframework.test.context.junit;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

import org.springframework.core.NestedExceptionUtils;

import static java.util.stream.Collectors.toList;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Utilities for tests that use JUnit's {@link EngineTestKit}.
 *
 * @author Sam Brannen
 * @since 6.2
 */
public class EngineTestKitUtils {

	public static Events executeTestsForClass(Class<?> testClass) {
		return EngineTestKit.engine("junit-jupiter")
				.selectors(selectClass(testClass))
				.execute()
				.testEvents();
	}

	/**
	 * Create a new {@link Condition} that matches if and only if a
	 * {@link Throwable}'s root {@linkplain Throwable#getCause() cause} matches
	 * all supplied conditions.
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public static Condition<Throwable> rootCause(Condition<Throwable>... conditions) {
		List<Condition<Throwable>> list = Arrays.stream(conditions)
				.map(EngineTestKitUtils::rootCause)
				.collect(toList());

		return Assertions.allOf(list);
	}

	private static Condition<Throwable> rootCause(Condition<Throwable> condition) {
		return new Condition<>(throwable -> condition.matches(NestedExceptionUtils.getRootCause(throwable)),
				"throwable root cause matches %s", condition);
	}

}
