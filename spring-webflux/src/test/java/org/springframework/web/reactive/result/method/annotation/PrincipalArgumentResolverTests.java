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

import java.security.Principal;

import io.reactivex.Single;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link PrincipalArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class PrincipalArgumentResolverTests {

	private final PrincipalArgumentResolver resolver =
			new PrincipalArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	public void supportsParameter() throws Exception {
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(Principal.class)));
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(Mono.class, Principal.class)));
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(Single.class, Principal.class)));
	}


	@Test
	public void resolverArgument() throws Exception {

		BindingContext context = new BindingContext();
		Principal user = () -> "Joe";
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
				.mutate().principal(Mono.just(user)).build();

		MethodParameter param = this.testMethod.arg(Principal.class);
		Object actual = this.resolver.resolveArgument(param, context, exchange).block();
		assertSame(user, actual);

		param = this.testMethod.arg(Mono.class, Principal.class);
		actual = this.resolver.resolveArgument(param, context, exchange).block();
		assertTrue(Mono.class.isAssignableFrom(actual.getClass()));
		assertSame(user, ((Mono<?>) actual).block());

		param = this.testMethod.arg(Single.class, Principal.class);
		actual = this.resolver.resolveArgument(param, context, exchange).block();
		assertTrue(Single.class.isAssignableFrom(actual.getClass()));
		assertSame(user, ((Single<?>) actual).blockingGet());
	}


	@SuppressWarnings("unused")
	void handle(
			Principal user,
			Mono<Principal> userMono,
			Single<Principal> singleUser) {
	}

}
