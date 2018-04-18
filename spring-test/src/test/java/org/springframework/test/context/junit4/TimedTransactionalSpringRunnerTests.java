/*
 * Copyright 2002-2016 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.annotation.Repeat;
import org.springframework.test.annotation.Timed;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.transaction.TransactionTestUtils.*;

/**
 * JUnit 4 based integration test which verifies support of Spring's
 * {@link Transactional &#64;Transactional} annotation in conjunction
 * with {@link Timed &#64;Timed} and JUnit 4's {@link Test#timeout()
 * timeout} attribute.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("transactionalTests-context.xml")
@Transactional
public class TimedTransactionalSpringRunnerTests {

	@Test
	@Timed(millis = 10000)
	@Repeat(5)
	public void transactionalWithSpringTimeout() {
		assertInTransaction(true);
	}

	@Test(timeout = 10000)
	@Repeat(5)
	public void transactionalWithJUnitTimeout() {
		assertInTransaction(true);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Timed(millis = 10000)
	@Repeat(5)
	public void notTransactionalWithSpringTimeout() {
		assertInTransaction(false);
	}

	@Test(timeout = 10000)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Repeat(5)
	public void notTransactionalWithJUnitTimeout() {
		assertInTransaction(false);
	}

}
