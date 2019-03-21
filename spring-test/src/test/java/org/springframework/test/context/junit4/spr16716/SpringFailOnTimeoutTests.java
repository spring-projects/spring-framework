/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.junit4.spr16716;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.model.Statement;
import org.mockito.stubbing.Answer;

import org.springframework.test.context.junit4.statements.SpringFailOnTimeout;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SpringFailOnTimeout}.
 *
 * @author Igor Suhorukov
 * @author Sam Brannen
 * @since 4.3.17
 */
public class SpringFailOnTimeoutTests {

	private Statement statement = mock(Statement.class);

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void nullNextStatement() throws Throwable {
		exception.expect(IllegalArgumentException.class);
		new SpringFailOnTimeout(null, 1);
	}

	@Test
	public void negativeTimeout() throws Throwable {
		exception.expect(IllegalArgumentException.class);
		new SpringFailOnTimeout(statement, -1);
	}

	@Test
	public void userExceptionPropagates() throws Throwable {
		doThrow(new Boom()).when(statement).evaluate();

		exception.expect(Boom.class);
		new SpringFailOnTimeout(statement, 1).evaluate();
	}

	@Test
	public void timeoutExceptionThrownIfNoUserException() throws Throwable {
		doAnswer((Answer<Void>) invocation -> {
			TimeUnit.MILLISECONDS.sleep(50);
			return null;
		}).when(statement).evaluate();

		exception.expect(TimeoutException.class);
		new SpringFailOnTimeout(statement, 1).evaluate();
	}

	@Test
	public void noExceptionThrownIfNoUserExceptionAndTimeoutDoesNotOccur() throws Throwable {
		doAnswer((Answer<Void>) invocation -> {
			return null;
		}).when(statement).evaluate();

		new SpringFailOnTimeout(statement, 100).evaluate();
	}

	@SuppressWarnings("serial")
	private static class Boom extends RuntimeException {
	}

}
