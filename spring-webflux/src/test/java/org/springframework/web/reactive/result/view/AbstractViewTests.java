/*
 * Copyright 2002-2019 the original author or authors.
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

import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.validation.BindingResult;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AbstractView}.
 *
 * @author Sebastien Deleuze
 */
public class AbstractViewTests {

	private MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));


	@Test
	@SuppressWarnings("unchecked")
	public void resolveAsyncAttributes() {

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
		assertEquals(testBean1, outMap.get("attr1"));
		assertEquals(Arrays.asList(testBean1, testBean2), outMap.get("attr2"));
		assertEquals(testBean2, outMap.get("attr3"));
		assertEquals(Arrays.asList(testBean1, testBean2), outMap.get("attr4"));
		assertNull(outMap.get("attr5"));

		assertNotNull(outMap.get(BindingResult.MODEL_KEY_PREFIX + "attr1"));
		assertNotNull(outMap.get(BindingResult.MODEL_KEY_PREFIX + "attr3"));
		assertNull(outMap.get(BindingResult.MODEL_KEY_PREFIX + "attr2"));
		assertNull(outMap.get(BindingResult.MODEL_KEY_PREFIX + "attr4"));
		assertNull(outMap.get(BindingResult.MODEL_KEY_PREFIX + "attr5"));
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
