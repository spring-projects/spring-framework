/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.concurrent;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
@SuppressWarnings("deprecation")
class FutureAdapterTests {

	private Future<Integer> adaptee = mock();

	private FutureAdapter<String, Integer> adapter = new FutureAdapter<>(adaptee) {
		@Override
		protected String adapt(Integer adapteeResult) {
			return adapteeResult.toString();
		}
	};



	@Test
	void cancel() {
		given(adaptee.cancel(true)).willReturn(true);
		boolean result = adapter.cancel(true);
		assertThat(result).isTrue();
	}

	@Test
	void isCancelled() {
		given(adaptee.isCancelled()).willReturn(true);
		boolean result = adapter.isCancelled();
		assertThat(result).isTrue();
	}

	@Test
	void isDone() {
		given(adaptee.isDone()).willReturn(true);
		boolean result = adapter.isDone();
		assertThat(result).isTrue();
	}

	@Test
	void get() throws Exception {
		given(adaptee.get()).willReturn(42);
		String result = adapter.get();
		assertThat(result).isEqualTo("42");
	}

	@Test
	void getTimeOut() throws Exception {
		given(adaptee.get(1, TimeUnit.SECONDS)).willReturn(42);
		String result = adapter.get(1, TimeUnit.SECONDS);
		assertThat(result).isEqualTo("42");
	}

}
