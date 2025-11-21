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

package org.springframework.test.context.testng;

import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.xml.XmlSuite.ParallelMode;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for concurrent TestNG tests.
 *
 * @author Sam Brannen
 * @since 6.2.12
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/35528">gh-35528</a>
 */
class TestNGConcurrencyTests {

	@org.junit.jupiter.api.Test
	void runTestsInParallel() throws Exception {
		TrackingTestNGTestListener listener = new TrackingTestNGTestListener();

		TestNG testNG = new TestNG();
		testNG.addListener(listener);
		testNG.setTestClasses(new Class<?>[] { ConcurrentTestCase.class });
		testNG.setParallel(ParallelMode.METHODS);
		testNG.setThreadCount(5);
		testNG.setVerbose(0);
		testNG.run();

		assertThat(listener.testStartCount.get()).as("tests started").isEqualTo(10);
		assertThat(listener.testSuccessCount.get()).as("successful tests").isEqualTo(10);
		assertThat(listener.testFailureCount.get()).as("failed tests").isEqualTo(0);
		assertThat(listener.failedConfigurationsCount.get()).as("failed configurations").isEqualTo(0);
		assertThat(listener.throwables).isEmpty();
	}


	@ContextConfiguration
	static class ConcurrentTestCase extends AbstractTestNGSpringContextTests {

		@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Message1")
		public void message1() {
			throw new RuntimeException("Message1");
		}

		@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Message2")
		public void message2() {
			throw new RuntimeException("Message2");
		}

		@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Message3")
		public void message3() {
			throw new RuntimeException("Message3");
		}

		@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Message4")
		public void message4() {
			throw new RuntimeException("Message4");
		}

		@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Message5")
		public void message5() {
			throw new RuntimeException("Message5");
		}

		@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Message6")
		public void message6() {
			throw new RuntimeException("Message6");
		}

		@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Message7")
		public void message7() {
			throw new RuntimeException("Message7");
		}

		@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Message8")
		public void message8() {
			throw new RuntimeException("Message8");
		}

		@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Message9")
		public void message9() {
			throw new RuntimeException("Message9");
		}

		@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Message10")
		public void message10() {
			throw new RuntimeException("Message10");
		}


		@Configuration
		static class Config {
		}

	}

}
