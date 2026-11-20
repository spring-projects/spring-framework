/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void error() {
		FailFastProblemReporter reporter = new FailFastProblemReporter();
		assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
				reporter.error(new Problem("VGER", new Location(new DescriptiveResource("here")),
						null, new IllegalArgumentException())));
	}

	@Test
	void warn() {
		Problem problem = new Problem("VGER", new Location(new DescriptiveResource("here")),
				null, new IllegalArgumentException());

		Log log = mock();

		FailFastProblemReporter reporter = new FailFastProblemReporter();
		reporter.setLogger(log);
		reporter.warning(problem);

		verify(log).warn(any(), isA(IllegalArgumentException.class));
	}

}
