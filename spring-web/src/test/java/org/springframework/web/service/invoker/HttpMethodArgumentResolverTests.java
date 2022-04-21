/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.service.invoker;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.web.service.annotation.GetRequest;
import org.springframework.web.service.annotation.HttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpMethodArgumentResolver}.
 *
 * @author Olga Maciaszek-Sharma
 */
class HttpMethodArgumentResolverTests extends HttpServiceMethodTestSupport {

	private final Service service = createService(Service.class,
			Collections.singletonList(new HttpMethodArgumentResolver()));

	@Test
	void shouldResolveRequestMethodFromArgument() {
		Mono<Void> execution = this.service.execute(HttpMethod.GET);

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getHttpMethod()).isEqualTo(HttpMethod.GET);
	}

	@Test
	void shouldIgnoreArgumentsNotMatchingType() {
		Mono<Void> execution = this.service.execute("test");

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getHttpMethod()).isNull();
	}

	@Test
	void shouldOverrideMethodAnnotationWithMethodArgument() {
		Mono<Void> execution = this.service.executeGet(HttpMethod.POST);

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getHttpMethod()).isEqualTo(HttpMethod.POST);
	}

	@Test
	void shouldIgnoreNullValue() {
		Mono<Void> execution = this.service.executeForNull(null);

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getHttpMethod()).isNull();
	}


	private interface Service {

		@HttpRequest
		Mono<Void> execute(HttpMethod method);

		@GetRequest
		Mono<Void> executeGet(HttpMethod method);

		@HttpRequest
		Mono<Void> execute(String test);

		@HttpRequest
		Mono<Void> execute(HttpMethod firstMethod, HttpMethod secondMethod);

		@HttpRequest
		Mono<Void> executeForNull(@Nullable HttpMethod method);
	}

}
