/*
 * Copyright 2002-2009 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Test-specific annotation to indicate that a test method has to finish
 * execution in a {@link #millis() specified time period}.
 * </p>
 * <p>
 * If the text execution takes longer than the specified time period, then the
 * test is to be considered failed.
 * </p>
 * <p>
 * Note that the time period includes execution of the test method itself, any
 * {@link Repeat repetitions} of the test, and any <em>set up</em> or
 * <em>tear down</em> of the test fixture.
 * </p>
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 * @see Repeat
 * @see AbstractAnnotationAwareTransactionalTests
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Timed {

	/**
	 * The maximum amount of time (in milliseconds) that a test execution can
	 * take without being marked as failed due to taking too long.
	 */
	long millis();

}
