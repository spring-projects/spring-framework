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

package org.springframework.web.reactive.result.view;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindingResult;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractView}.
 *
 * @author Sebastien Deleuze
 */
class AbstractViewTests {

	private MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));


	@Test
	void resolveAsyncAttributes() {

		TestBean testBean1 = new TestBean("Bean1");
		TestBean testBean2 = new TestBean("Bean2");

		Map<String, Object> inMap = new HashMap<>();
		inMap.put("attr1", Mono.just(testBean1).delayElement(Duration.ofMillis(10)));
		inMap.put("attr2", Flux.just(testBean1, testBean2).delayElements(Duration.ofMillis(10)));
		inMap.put("attr3", Single.just(testBean2));
		inMap.put("attr4", Observable.just(testBean1, testBean2));
		inMap.put("attr5", Mono.empty());

		this.exchange.getAttributes().put(View.BINDING_CONTEXT_ATTRIBUTE, new BindingContext());

		TestView view = new TestView();
		StepVerifier.create(view.render(inMap, null, this.exchange)).verifyComplete();

		Map<String, Object> outMap = view.attributes;
		assertThat(outMap.get("attr1")).isEqualTo(testBean1);
		assertThat(outMap.get("attr2")).isEqualTo(Arrays.asList(testBean1, testBean2));
		assertThat(outMap.get("attr3")).isEqualTo(testBean2);
		assertThat(outMap.get("attr4")).isEqualTo(Arrays.asList(testBean1, testBean2));
		assertThat(outMap.get("attr5")).isNull();

		assertThat(outMap.get(BindingResult.MODEL_KEY_PREFIX + "attr1")).isNotNull();
		assertThat(outMap.get(BindingResult.MODEL_KEY_PREFIX + "attr3")).isNotNull();
		assertThat(outMap.get(BindingResult.MODEL_KEY_PREFIX + "attr2")).isNull();
		assertThat(outMap.get(BindingResult.MODEL_KEY_PREFIX + "attr4")).isNull();
		assertThat(outMap.get(BindingResult.MODEL_KEY_PREFIX + "attr5")).isNull();
	}

	private static class TestView extends AbstractView {

		private Map<String, Object> attributes;

		@Override
		protected Mono<Void> renderInternal(Map<String, Object> renderAttributes,
				@Nullable MediaType contentType, ServerWebExchange exchange) {

			this.attributes = renderAttributes;
			return Mono.empty();
		}
	}

}
