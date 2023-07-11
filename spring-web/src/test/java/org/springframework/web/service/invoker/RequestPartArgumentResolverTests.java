/*
 * Copyright 2002-2023 the original author or authors.
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

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.service.annotation.PostExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequestPartArgumentResolver}.
 *
 * <p>Additional tests for this resolver:
 * <ul>
 * <li>Base class functionality in {@link NamedValueArgumentResolverTests}
 * <li>Form data vs query params in {@link HttpRequestValuesTests}
 * </ul>
 *
 * @author Rossen Stoyanchev
 */
class RequestPartArgumentResolverTests {

	private final TestReactorExchangeAdapter client = new TestReactorExchangeAdapter();

	private final Service service =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(Service.class);


	// Base class functionality should be tested in NamedValueArgumentResolverTests.
	// Form data vs query params tested in HttpRequestValuesTests.

	@Test
	void requestPart() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		HttpEntity<String> part2 = new HttpEntity<>("part 2", headers);
		this.service.postMultipart("part 1", part2, Mono.just("part 3"));

		Object body = this.client.getRequestValues().getBodyValue();
		assertThat(body).isInstanceOf(MultiValueMap.class);

		@SuppressWarnings("unchecked")
		MultiValueMap<String, HttpEntity<?>> map = (MultiValueMap<String, HttpEntity<?>>) body;
		assertThat(map.getFirst("part1").getBody()).isEqualTo("part 1");
		assertThat(map.getFirst("part2")).isEqualTo(part2);
		assertThat(((Mono<?>) map.getFirst("part3").getBody()).block()).isEqualTo("part 3");
	}


	private interface Service {

		@PostExchange
		void postMultipart(
				@RequestPart String part1, @RequestPart HttpEntity<String> part2,
				@RequestPart Mono<String> part3);

	}

}
