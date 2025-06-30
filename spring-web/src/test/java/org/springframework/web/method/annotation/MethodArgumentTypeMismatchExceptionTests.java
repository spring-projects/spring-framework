/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.method.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MethodArgumentTypeMismatchExceptionTests {

	@Test //gh-33573
	void messageIncludesParameterName() {
		@SuppressWarnings("DataFlowIssue")
		MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
				"mismatched value", Integer.class, "paramOne", null, null);

		assertThat(ex).hasMessage("Method parameter 'paramOne': Failed to convert value of type " +
				"'java.lang.String' to required type 'java.lang.Integer'");
	}

}
