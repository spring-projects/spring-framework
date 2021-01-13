/*
 * Copyright 2002-2020 the original author or authors.
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
 * Unit tests for {@link WebSessionMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSessionMethodArgumentResolverTests {

	private final WebSessionMethodArgumentResolver resolver =
			new WebSessionMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	public void supportsParameter() {
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(WebSession.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(Mono.class, WebSession.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(Single.class, WebSession.class))).isTrue();
	}


	@Test
	public void resolverArgument() {

		BindingContext context = new BindingContext();
		WebSession session = mock(WebSession.class);
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
