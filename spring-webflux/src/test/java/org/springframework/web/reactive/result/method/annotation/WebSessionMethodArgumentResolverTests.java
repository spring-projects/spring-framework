/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebSessionMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class WebSessionMethodArgumentResolverTests {

	private final WebSessionMethodArgumentResolver resolver =
			new WebSessionMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	void supportsParameter() {
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(WebSession.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(Mono.class, WebSession.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(Single.class, WebSession.class))).isTrue();
	}


	@Test
	void resolverArgument() {

		BindingContext context = new BindingContext();
		WebSession session = mock();
		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		ServerWebExchange exchange = MockServerWebExchange.builder(request).session(session).build();

		MethodParameter param = this.testMethod.arg(WebSession.class);
		Object actual = this.resolver.resolveArgument(param, context, exchange).block();
		assertThat(actual).isSameAs(session);

		param = this.testMethod.arg(Mono.class, WebSession.class);
		actual = this.resolver.resolveArgument(param, context, exchange).block();
		assertThat(actual).isNotNull();
		assertThat(Mono.class.isAssignableFrom(actual.getClass())).isTrue();
		assertThat(((Mono<?>) actual).block()).isSameAs(session);

		param = this.testMethod.arg(Single.class, WebSession.class);
		actual = this.resolver.resolveArgument(param, context, exchange).block();
		assertThat(actual).isNotNull();
		assertThat(Single.class.isAssignableFrom(actual.getClass())).isTrue();
		assertThat(((Single<?>) actual).blockingGet()).isSameAs(session);
	}


	@SuppressWarnings("unused")
	void handle(
			WebSession user,
			Mono<WebSession> userMono,
			Single<WebSession> singleUser) {
	}

}
