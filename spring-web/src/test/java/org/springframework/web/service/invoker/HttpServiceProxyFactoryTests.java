/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import org.aopalliance.intercept.MethodInterceptor;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link HttpServiceProxyFactory}.
 *
 * @author Rossen Stoyanchev
 */
class HttpServiceProxyFactoryTests {

	@Test
	void httpExchangeAdapterDecorator() {

		HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(mock(HttpExchangeAdapter.class))
				.exchangeAdapterDecorator(TestDecorator::new)
				.build();

		Service service = factory.createClient(Service.class);
		assertThat(service.execute()).isEqualTo("decorated");
	}

	@Test
	void proxyFactoryCustomizer() {
		var interceptor = (MethodInterceptor) invocation -> "intercepted";
		var proxyFactory = HttpServiceProxyFactory.builderFor(mock(HttpExchangeAdapter.class))
				.proxyFactoryCustomizer((factory, serviceType) -> factory.addAdvice(0, interceptor))
				.build();

		String result = proxyFactory.createClient(Service.class).execute();

		assertThat(result).isEqualTo("intercepted");
	}


	private interface Service {

		@GetExchange
		String execute();
	}


	private static class TestDecorator extends HttpExchangeAdapterDecorator {

		public TestDecorator(HttpExchangeAdapter delegate) {
			super(delegate);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> @Nullable T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
			return (T) "decorated";
		}
	}

}
