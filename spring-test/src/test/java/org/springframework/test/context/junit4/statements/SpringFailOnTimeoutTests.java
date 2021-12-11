/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.junit4.statements;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.junit.runners.model.Statement;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link SpringFailOnTimeout}.
 *
 * @author Igor Suhorukov
 * @author Sam Brannen
 * @since 4.3.17
 */
public class SpringFailOnTimeoutTests {

	private final Statement statement = mock(Statement.class);


	@Test
	public void nullNextStatement() throws Throwable {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new SpringFailOnTimeout(null, 1));
	}

	@Test
	public void negativeTimeout() throws Throwable {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new SpringFailOnTimeout(statement, -1));
	}

	@Test
	public void userExceptionPropagates() throws Throwable {
		willThrow(new Boom()).given(statement).evaluate();

		assertThatExceptionOfType(Boom.class).isThrownBy(() ->
				new SpringFailOnTimeout(statement, 1).evaluate());
	}

	@Test
	public void timeoutExceptionThrownIfNoUserException() throws Throwable {
		willAnswer((Answer<Void>) invocation -> {
			TimeUnit.MILLISECONDS.sleep(50);
			return null;
		}).given(statement).evaluate();

		assertThatExceptionOfType(TimeoutException.class).isThrownBy(() ->
		new SpringFailOnTimeout(statement, 1).evaluate());
	}

	@Test
	public void noExceptionThrownIfNoUserExceptionAndTimeoutDoesNotOccur() throws Throwable {
		willAnswer((Answer<Void>) invocation -> null).given(statement).evaluate();
		new SpringFailOnTimeout(statement, 100).evaluate();
	}

	@SuppressWarnings("serial")
	private static class Boom extends RuntimeException {
	}

}
