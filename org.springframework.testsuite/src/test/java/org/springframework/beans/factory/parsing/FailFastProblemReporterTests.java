/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.easymock.AbstractMatcher;
import org.easymock.MockControl;

import org.springframework.core.io.DescriptiveResource;
import org.springframework.test.AssertThrows;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public final class FailFastProblemReporterTests extends TestCase {

	public void testError() throws Exception {
		new AssertThrows(BeanDefinitionParsingException.class) {
			public void test() throws Exception {
				FailFastProblemReporter reporter = new FailFastProblemReporter();
				reporter.error(new Problem("VGER", new Location(new DescriptiveResource("here")),
						null, new IllegalArgumentException()));
			}
		}.runTest();
	}

	public void testWarn() throws Exception {
		IllegalArgumentException rootCause = new IllegalArgumentException();
		Problem problem = new Problem("VGER", new Location(new DescriptiveResource("here")),
				null, new IllegalArgumentException());

		MockControl mockLog = MockControl.createControl(Log.class);
		Log log = (Log) mockLog.getMock();
		log.warn(null, rootCause);
		mockLog.setMatcher(new AbstractMatcher() {
			public boolean matches(Object[] expected, Object[] actual) {
				Assert.assertEquals(2, actual.length);
				Assert.assertTrue(actual[1] instanceof IllegalArgumentException);
				return true;
			}
		});
		mockLog.replay();

		FailFastProblemReporter reporter = new FailFastProblemReporter();
		reporter.setLogger(log);
		reporter.warning(problem);

		mockLog.verify();
	}

}
