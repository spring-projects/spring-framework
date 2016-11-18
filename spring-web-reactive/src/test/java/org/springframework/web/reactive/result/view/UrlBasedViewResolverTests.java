/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;

import reactor.test.StepVerifier;

/**
 * Unit tests for {@link UrlBasedViewResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class UrlBasedViewResolverTests {

	private UrlBasedViewResolver resolver;

	@Before
	public void setUp() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.refresh();
		resolver = new UrlBasedViewResolver();
		resolver.setApplicationContext(context);
	}

	@Test
	public void viewNames() throws Exception {
		resolver.setViewClass(TestView.class);
		resolver.setViewNames("my*");

		Mono<View> mono = resolver.resolveViewName("my-view", Locale.US);
		assertNotNull(mono.block());

		mono = resolver.resolveViewName("not-my-view", Locale.US);
		assertNull(mono.block());
	}

	@Test
	public void redirectView() throws Exception {
		Mono<View> mono = resolver.resolveViewName("redirect:foo", Locale.US);
		assertNotNull(mono.block());
		StepVerifier.create(mono)
				.consumeNextWith(view -> {
					assertEquals(RedirectView.class, view.getClass());
					RedirectView redirectView = (RedirectView) view;
					assertEquals(redirectView.getUrl(), "foo");
					assertEquals(redirectView.getStatusCode(), HttpStatus.SEE_OTHER);
				})
				.expectComplete();
	}

	@Test
	public void customizedRedirectView() throws Exception {
		resolver.setRedirectViewProvider(url -> RedirectView.builder(url).statusCode(HttpStatus.FOUND).build());
		Mono<View> mono = resolver.resolveViewName("redirect:foo", Locale.US);
		assertNotNull(mono.block());
		StepVerifier.create(mono)
				.consumeNextWith(view -> {
					assertEquals(RedirectView.class, view.getClass());
					RedirectView redirectView = (RedirectView) view;
					assertEquals(redirectView.getUrl(), "foo");
					assertEquals(redirectView.getStatusCode(), HttpStatus.FOUND);
				})
				.expectComplete();
	}

	private static class TestView extends AbstractUrlBasedView {

		@Override
		public boolean checkResourceExists(Locale locale) throws Exception {
			return true;
		}

		@Override
		protected Mono<Void> renderInternal(Map<String, Object> attributes, MediaType contentType,
				ServerWebExchange exchange) {
			return Mono.empty();
		}
	}

}
