/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.server;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;

/**
 * Implementation of {@link RouterFunctions.Visitor} that creates a formatted
 * string representation of router functions.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class ToStringVisitor implements RouterFunctions.Visitor, RequestPredicates.Visitor {

	private final StringBuilder builder = new StringBuilder();

	private int indent = 0;


	// RouterFunctions.Visitor

	@Override
	public void startNested(RequestPredicate predicate) {
		indent();
		predicate.accept(this);
		this.builder.append(" => {\n");
		this.indent++;
	}

	@Override
	public void endNested(RequestPredicate predicate) {
		this.indent--;
		indent();
		this.builder.append("}\n");
	}

	@Override
	public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
		indent();
		predicate.accept(this);
		this.builder.append(" -> ");
		this.builder.append(handlerFunction).append('\n');
	}

	@Override
	public void resources(Function<ServerRequest, Mono<Resource>> lookupFunction) {
		indent();
		this.builder.append(lookupFunction).append('\n');
	}

	@Override
	public void attributes(Map<String, Object> attributes) {
	}

	@Override
	public void unknown(RouterFunction<?> routerFunction) {
		indent();
		this.builder.append(routerFunction);
	}

	private void indent() {
		this.builder.append(" ".repeat(Math.max(0, this.indent)));
	}


	// RequestPredicates.Visitor

	@Override
	public void method(Set<HttpMethod> methods) {
		if (methods.size() == 1) {
			this.builder.append(methods.iterator().next());
		}
		else {
			this.builder.append(methods);
		}
	}

	@Override
	public void path(String pattern) {
		this.builder.append(pattern);
	}

	@SuppressWarnings("removal")
	@Deprecated(since = "7.0", forRemoval = true)
	@Override
	public void pathExtension(String extension) {
		this.builder.append(String.format("*.%s", extension));
	}

	@Override
	public void header(String name, String value) {
		this.builder.append(String.format("%s: %s", name, value));
	}

	@Override
	public void queryParam(String name, String value) {
		this.builder.append(String.format("?%s == %s", name, value));
	}

	@Override
	public void version(String version) {
		this.builder.append(String.format("version: %s", version));
	}

	@Override
	public void startAnd() {
		this.builder.append('(');
	}

	@Override
	public void and() {
		this.builder.append(" && ");
	}

	@Override
	public void endAnd() {
		this.builder.append(')');
	}

	@Override
	public void startOr() {
		this.builder.append('(');
	}

	@Override
	public void or() {
		this.builder.append(" || ");

	}

	@Override
	public void endOr() {
		this.builder.append(')');
	}

	@Override
	public void startNegate() {
		this.builder.append("!(");
	}

	@Override
	public void endNegate() {
		this.builder.append(')');
	}

	@Override
	public void unknown(RequestPredicate predicate) {
		this.builder.append(predicate);
	}

	@Override
	public String toString() {
		String result = this.builder.toString();
		if (result.endsWith("\n")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

}
