/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.scheduling.annotation;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests use of @BeforeSchedule annotation.
 *
 * @author Nikolai Bogdanov
 * @since 5.1
 */
public class BeforeScheduleTests {
	private AnnotationConfigApplicationContext ctx;

	@After
	public void tearDown() {
		if (ctx != null) {
			ctx.close();
		}
	}

	@Test
	public void shouldAffectAllSchedulesInClass() throws InterruptedException {
		Assume.group(TestGroup.PERFORMANCE);

		ctx = new AnnotationConfigApplicationContext(TwoSchedulesDisabled.class);
		Thread.sleep(100);

		assertEquals(0, ctx.getBean(TwoSchedulesDisabled.class).counter1().get());
		assertEquals(0, ctx.getBean(TwoSchedulesDisabled.class).counter2().get());
	}

	@Test
	public void shouldNotValidateCron() {
		ctx = new AnnotationConfigApplicationContext(WrongCronExpressionDisabled.class);
		//ok!
	}

	@Test
	public void shouldStartExecutionInSimpleExample() throws InterruptedException {
		Assume.group(TestGroup.PERFORMANCE);

		ctx = new AnnotationConfigApplicationContext(SimpleEnabled.class);

		Thread.sleep(100);

		assertTrue(0 < ctx.getBean(SimpleEnabled.class).counter().get());
	}

	@Test
	public void shouldFailStartupIfErrorInBeforeExecution() {
		try {
			ctx = new AnnotationConfigApplicationContext(FailOnStart.class);
			fail();
		} catch (BeanCreationException bce) {
			assertEquals(bce.getCause().getClass(), MyTestException.class);
		}
	}

	@Test
	public void shouldFailStartupIfNullReturned() {
		try {
			ctx = new AnnotationConfigApplicationContext(ReturnNull.class);
			fail();
		} catch (BeanCreationException bce) {
			assertEquals(bce.getCause().getClass(), NullPointerException.class);
		}
	}


	@Configuration
	@EnableScheduling
	static class TwoSchedulesDisabled {
		@Bean
		public AtomicInteger counter1() {
			return new AtomicInteger();
		}

		public AtomicInteger counter2() {
			return new AtomicInteger();
		}

		@Bean
		public Object bean() {
			return new Object() {
				@Scheduled(fixedDelay = 10)
				public void scheduled1() {
					counter1().incrementAndGet();
				}

				@Scheduled(fixedDelay = 20)
				public void scheduled2() {
					counter2().incrementAndGet();
				}

				@BeforeSchedule
				public boolean start() {
					return false;
				}
			};
		}
	}

	@Configuration
	@EnableScheduling
	static class WrongCronExpressionDisabled {
		@Scheduled(cron = "wrong")
		public void job() {
			//nothing to do
		}

		@BeforeSchedule
		public boolean start() {
			return false;
		}
	}

	@Configuration
	@EnableScheduling
	static class SimpleEnabled {
		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Scheduled(fixedDelay = 10)
		public void job() {
			counter().incrementAndGet();
		}

		@BeforeSchedule
		public Boolean start() {
			return true;
		}
	}

	@Configuration
	@EnableScheduling
	static class FailOnStart {
		@Scheduled(fixedDelay = 10)
		public void job() {
			// nothing to do
		}

		@BeforeSchedule
		public Boolean start() {
			throw new MyTestException();
		}
	}

	@Configuration
	@EnableScheduling
	static class ReturnNull {
		@Scheduled(fixedDelay = 10)
		public void job() {
			// nothing to do
		}

		@BeforeSchedule
		public Boolean start() {
			return null;
		}
	}

	static class MyTestException extends RuntimeException {
	}
}