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

package org.springframework.web.reactive.function.server;

import java.util.Map;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author Arjen Poutsma
 */
class AttributesTestVisitor implements RouterFunctions.Visitor {

	@Nullable
	private Map<String, Object> attributes;

	private int visitCount;

	public int visitCount() {
		return this.visitCount;
	}

	@Override
	public void startNested(RequestPredicate predicate) {
	}

	@Override
	public void endNested(RequestPredicate predicate) {
	}

	@Override
	public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
		assertThat(this.attributes).isNotNull();
		this.attributes = null;
	}

	@Override
	public void resources(Function<ServerRequest, Mono<Resource>> lookupFunction) {
	}

	@Override
	public void attributes(Map<String, Object> attributes) {
		assertThat(attributes).containsExactly(entry("foo", "bar"), entry("baz", "qux"));
		this.attributes = attributes;
		this.visitCount++;
	}

	@Override
	public void unknown(RouterFunction<?> routerFunction) {

	}
}
