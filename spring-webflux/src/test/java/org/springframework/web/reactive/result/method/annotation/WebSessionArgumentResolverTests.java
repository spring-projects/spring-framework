/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.web.reactive.result.method.annotation;

import java.time.Clock;

import io.reactivex.Single;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSession;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link WebSessionArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class WebSessionArgumentResolverTests {

	private final WebSessionArgumentResolver resolver =
			new WebSessionArgumentResolver(new ReactiveAdapterRegistry());

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	public void supportsParameter() throws Exception {
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(WebSession.class)));
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(Mono.class, WebSession.class)));
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(Single.class, WebSession.class)));
	}


	@Test
	public void resolverArgument() throws Exception {

		BindingContext context = new BindingContext();
		WebSession session = new DefaultWebSession("id", Clock.systemDefaultZone());
		WebSessionManager manager = exchange -> Mono.just(session);
		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		ServerWebExchange exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse(),
				manager, ServerCodecConfigurer.create());

		MethodParameter param = this.testMethod.arg(WebSession.class);
		Object actual = this.resolver.resolveArgument(param, context, exchange).block();
		assertSame(session, actual);

		param = this.testMethod.arg(Mono.class, WebSession.class);
		actual = this.resolver.resolveArgument(param, context, exchange).block();
		assertTrue(Mono.class.isAssignableFrom(actual.getClass()));
		assertSame(session, ((Mono<?>) actual).block());

		param = this.testMethod.arg(Single.class, WebSession.class);
		actual = this.resolver.resolveArgument(param, context, exchange).block();
		assertTrue(Single.class.isAssignableFrom(actual.getClass()));
		assertSame(session, ((Single<?>) actual).blockingGet());
	}


	@SuppressWarnings("unused")
	void handle(
			WebSession user,
			Mono<WebSession> userMono,
			Single<WebSession> singleUser) {
	}

}
