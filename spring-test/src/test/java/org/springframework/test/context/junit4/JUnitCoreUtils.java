/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context.junit4;

import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import static org.junit.Assert.*;

/**
 * Utility class for {@link org.junit.runner.JUnitCore}.
 *
 * @author Tadaya Tsuyukubo
 * @since 3.2
 */
public class JUnitCoreUtils {

	/**
	 * Verify {@link Result} instance returned from {@link org.junit.runner.JUnitCore} doesn't contain any failures.
	 *
	 * @param result test result to verify
	 */
	public static void verifyResult(Result result) {
		if (!result.wasSuccessful()) {
			StringBuilder sb = new StringBuilder("Invoked tests failed : ");
			for (Failure failure : result.getFailures()) {
				sb.append(failure.getMessage());
				fail(sb.toString());
			}
		}
	}

}
