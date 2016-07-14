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

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link UrlBasedViewResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class UrlBasedViewResolverTests {


	@Test
	public void viewNames() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.refresh();

		UrlBasedViewResolver resolver = new UrlBasedViewResolver();
		resolver.setViewClass(TestView.class);
		resolver.setViewNames("my*");
		resolver.setApplicationContext(context);

		Mono<View> mono = resolver.resolveViewName("my-view", Locale.US);
		assertNotNull(mono.block());

		mono = resolver.resolveViewName("not-my-view", Locale.US);
		assertNull(mono.block());
	}


	private static class TestView extends AbstractUrlBasedView {

		@Override
		public boolean checkResourceExists(Locale locale) throws Exception {
			return true;
		}

		@Override
		protected Mono<Void> renderInternal(Map<String, Object> attributes, ServerWebExchange exchange) {
			return Mono.empty();
		}
	}

}
