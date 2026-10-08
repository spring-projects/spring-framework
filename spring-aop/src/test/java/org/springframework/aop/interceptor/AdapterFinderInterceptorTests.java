/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aop.interceptor;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.AdapterFinderBean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for class {@link AdapterFinderInterceptor}
 *
 * @author Joe Chambers
 */
@ExtendWith(MockitoExtension.class)
class AdapterFinderInterceptorTests {

	@Mock
	EvenOddService evenService;

	@Mock
	EvenOddService oddService;

	@Spy
	EvenOddServiceFinder evenOddFinder = new EvenOddServiceFinder();

	Method expectedMethod;

	EvenOddService evenOddService;

	@BeforeEach
	void setup() throws Exception {
		evenOddService = AdapterFinderInterceptor.proxyOf(evenOddFinder, EvenOddService.class);
		expectedMethod = EvenOddService.class.getMethod("toMessage", int.class, String.class);
	}

	@Test
	void callEvenService() {
		String expectedMessage = "4 even message";

		given(evenService.toMessage(eq(4), eq("message"))).willReturn(expectedMessage);

		String actualMessage = evenOddService.toMessage(4, "message");

		assertThat(actualMessage)
						.isEqualTo(expectedMessage);

		verify(evenService).toMessage(eq(4), eq("message"));
		verify(oddService, never()).toMessage(anyInt(), anyString());
		verify(evenOddFinder).findAdapter(eq(expectedMethod), eq(new Object[] { 4, "message" }));
	}

	@Test
	void callOddService() {
		String expectedMessage = "5 odd message";

		given(oddService.toMessage(eq(5), eq("message")))
						.willReturn(expectedMessage);

		String actualMessage = evenOddService.toMessage(5, "message");

		assertThat(actualMessage)
						.isEqualTo(expectedMessage);

		verify(oddService).toMessage(eq(5), eq("message"));
		verify(evenService, never()).toMessage(anyInt(), anyString());
		verify(evenOddFinder).findAdapter(eq(expectedMethod), eq(new Object[] { 5, "message" }));
	}

	@Test
	void throwExceptionWhenNumberIsZero() {
		String expectedMessage = "Adapter not found: public abstract java.lang.String org.springframework.aop.interceptor.AdapterFinderInterceptorTests$EvenOddService.toMessage(int,java.lang.String)";

		assertThatThrownBy(() -> evenOddService.toMessage(0, "message"))
						.isInstanceOf(IllegalArgumentException.class)
						.hasMessage(expectedMessage)
						.hasNoCause();

		verify(evenService, never()).toMessage(anyInt(), anyString());
		verify(oddService, never()).toMessage(anyInt(), anyString());
		verify(evenOddFinder).findAdapter(eq(expectedMethod), eq(new Object[] { 0, "message" }));
	}

	protected interface EvenOddService {
		String toMessage(int number, String message);
	}

	protected class EvenOddServiceFinder implements AdapterFinderBean<EvenOddService> {
		@Override
		@Nullable
		public EvenOddService findAdapter(Method method, @Nullable Object[] args) {
			if (method.getParameterCount() > 0 && method.getParameterTypes()[0] == int.class && args[0] != null) {
				int number = (int) args[0];
				if (number != 0) {
					return ((number % 2 == 0) ? evenService : oddService);
				}
			}
			return null; // method not found, or 0.
		}
	}
}
