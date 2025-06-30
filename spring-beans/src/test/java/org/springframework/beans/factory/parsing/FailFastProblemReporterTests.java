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

package org.springframework.beans.factory.parsing;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.DescriptiveResource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class FailFastProblemReporterTests {

	@Test
	void testError() {
		FailFastProblemReporter reporter = new FailFastProblemReporter();
		assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
				reporter.error(new Problem("VGER", new Location(new DescriptiveResource("here")),
						null, new IllegalArgumentException())));
	}

	@Test
	void testWarn() {
		Problem problem = new Problem("VGER", new Location(new DescriptiveResource("here")),
				null, new IllegalArgumentException());

		Log log = mock();

		FailFastProblemReporter reporter = new FailFastProblemReporter();
		reporter.setLogger(log);
		reporter.warning(problem);

		verify(log).warn(any(), isA(IllegalArgumentException.class));
	}

}
