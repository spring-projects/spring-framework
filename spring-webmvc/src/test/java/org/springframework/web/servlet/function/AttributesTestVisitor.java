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

package org.springframework.web.servlet.function;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * @author Arjen Poutsma
 */
class AttributesTestVisitor implements RouterFunctions.Visitor {

	private Deque<Map<String, Object>> nestedAttributes = new LinkedList<>();

	@Nullable
	private Map<String, Object> attributes;

	private List<List<Map<String, Object>>> routerFunctionsAttributes = new LinkedList<>();

	private int visitCount;

	public List<List<Map<String, Object>>> routerFunctionsAttributes() {
		return this.routerFunctionsAttributes;
	}

	public int visitCount() {
		return this.visitCount;
	}

	@Override
	public void startNested(RequestPredicate predicate) {
		nestedAttributes.addFirst(attributes);
		attributes = null;
	}

	@Override
	public void endNested(RequestPredicate predicate) {
		attributes = nestedAttributes.removeFirst();
	}

	@Override
	public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
		Stream<Map<String, Object>> current = Optional.ofNullable(attributes).stream();
		Stream<Map<String, Object>> nested = nestedAttributes.stream().filter(Objects::nonNull);
		routerFunctionsAttributes.add(Stream.concat(current, nested).collect(Collectors.toUnmodifiableList()));
		attributes = null;
	}

	@Override
	public void resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
	}

	@Override
	public void attributes(Map<String, Object> attributes) {
		this.attributes = attributes;
		this.visitCount++;
	}

	@Override
	public void unknown(RouterFunction<?> routerFunction) {

	}
}
