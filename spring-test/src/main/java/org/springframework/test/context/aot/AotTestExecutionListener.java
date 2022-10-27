/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.aot;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.test.context.TestExecutionListener;

/**
 * {@code AotTestExecutionListener} is an extension of the {@link TestExecutionListener}
 * SPI that allows a listener to optionally provide ahead-of-time (AOT) support.
 *
 * @author Sam Brannen
 * @since 6.0
 */
public interface AotTestExecutionListener extends TestExecutionListener {

	/**
	 * Process the supplied test class ahead-of-time using the given
	 * {@link RuntimeHints} instance.
	 * <p>If possible, implementations should use the specified {@link ClassLoader}
	 * to determine if hints have to be contributed.
	 * @param runtimeHints the {@code RuntimeHints} to use
	 * @param testClass the test class to process
	 * @param classLoader the classloader to use
	 */
	void processAheadOfTime(RuntimeHints runtimeHints, Class<?> testClass, ClassLoader classLoader);

}
