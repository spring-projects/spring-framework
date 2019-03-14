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

package org.springframework.test.annotation;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * Collection of utility methods for working with Spring's core testing annotations.
 *
 * @author Sam Brannen
 * @since 4.2
 */
public abstract class TestAnnotationUtils {

	/**
	 * Get the {@code timeout} configured via the {@link Timed @Timed}
	 * annotation on the supplied {@code method}.
	 * <p>Negative configured values will be converted to {@code 0}.
	 * @return the configured timeout, or {@code 0} if the method is not
	 * annotated with {@code @Timed}
	 */
	public static long getTimeout(Method method) {
		Timed timed = AnnotatedElementUtils.findMergedAnnotation(method, Timed.class);
		return (timed == null ? 0 : Math.max(0, timed.millis()));
	}

	/**
	 * Get the repeat count configured via the {@link Repeat @Repeat}
	 * annotation on the supplied {@code method}.
	 * <p>Non-negative configured values will be converted to {@code 1}.
	 * @return the configured repeat count, or {@code 1} if the method is
	 * not annotated with {@code @Repeat}
	 */
	public static int getRepeatCount(Method method) {
		Repeat repeat = AnnotatedElementUtils.findMergedAnnotation(method, Repeat.class);
		if (repeat == null) {
			return 1;
		}
		return Math.max(1, repeat.value());
	}

}
