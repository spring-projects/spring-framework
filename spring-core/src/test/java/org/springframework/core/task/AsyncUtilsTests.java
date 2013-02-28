/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Phillip Webb
 */
public class AsyncUtilsTests {

	// FIXME tests

	@Mock
	private ExecutorService executor;

	@Mock
	private Future<?> future;

	private Callable<?> callable;

	@Before
	@SuppressWarnings("unchecked")
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		given(future.get()).willAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return callable.call();
			}
		});
		given(executor.submit((Callable) anyObject())).willAnswer(new Answer<Future<?>>() {
			@Override
			public Future<?> answer(InvocationOnMock invocation) throws Throwable {
				callable = (Callable<?>) invocation.getArguments()[0];
				return future;
			}
		});
	}

	@Test
	public void submitToClass() throws Exception {
		ExampleClass source = new ExampleClass();
		ExampleClass submitter = AsyncUtils.submitVia(executor, source);
		assertThat(callable, is(nullValue()));
		Result result = submitter.classMethod();
		verify(future, never()).get();
		assertThat(result.getResult(), equalTo("classResult"));
		verify(future).get();
	}

	static interface exampleInterface {

		Result interfaceMethod();
	}


	static class ExampleClass implements exampleInterface {

		public Result interfaceMethod() {
			return new ResultImpl("interfaceResult");
		}

		public Result classMethod() {
			return new ResultImpl("classResult");
		}
	}


	static final class ExampleFinalClass extends ExampleClass {
	}


	static interface Result {

		String getResult();
	}

	private static final class ResultImpl implements Result {

		private String result;

		public ResultImpl(String result) {
			this.result = result;
		}

		public String getResult() {
			return this.result;
		}

	}
}
