/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.function;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.Resource;

/**
 * @author Arjen Poutsma
 */
class AttributesTestVisitor implements RouterFunctions.Visitor {

	private final Deque<Map<String, Object>> nestedAttributes = new LinkedList<>();

	private final List<List<Map<String, Object>>> routerFunctionsAttributes = new LinkedList<>();

	private @Nullable Map<String, Object> attributes;

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
		routerFunctionsAttributes.add(Stream.concat(current, nested).toList());
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
