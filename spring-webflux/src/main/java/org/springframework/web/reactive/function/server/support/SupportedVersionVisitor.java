/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.server.support;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.accept.DefaultApiVersionStrategy;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * {@link RequestPredicates.Visitor} that discovers versions used in routes in
 * order to add them to the list of supported versions.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
final class SupportedVersionVisitor implements RouterFunctions.Visitor, RequestPredicates.Visitor {

	private final DefaultApiVersionStrategy versionStrategy;


	SupportedVersionVisitor(DefaultApiVersionStrategy versionStrategy) {
		this.versionStrategy = versionStrategy;
	}


	// RouterFunctions.Visitor

	@Override
	public void startNested(RequestPredicate predicate) {
		predicate.accept(this);
	}

	@Override
	public void endNested(RequestPredicate predicate) {
	}

	@Override
	public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
		predicate.accept(this);
	}

	@Override
	public void resources(Function<ServerRequest, Mono<Resource>> lookupFunction) {
	}

	@Override
	public void attributes(Map<String, Object> attributes) {
	}

	@Override
	public void unknown(RouterFunction<?> routerFunction) {
	}


	// RequestPredicates.Visitor

	@Override
	public void method(Set<HttpMethod> methods) {
	}

	@Override
	public void path(String pattern) {
	}

	@SuppressWarnings("removal")
	@Override
	public void pathExtension(String extension) {
	}

	@Override
	public void header(String name, String value) {
	}

	@Override
	public void queryParam(String name, String value) {
	}

	@Override
	public void version(String version) {
		this.versionStrategy.addMappedVersion(
				(version.endsWith("+") ? version.substring(0, version.length() - 1) : version));
	}

	@Override
	public void startAnd() {
	}

	@Override
	public void and() {
	}

	@Override
	public void endAnd() {
	}

	@Override
	public void startOr() {
	}

	@Override
	public void or() {
	}

	@Override
	public void endOr() {
	}

	@Override
	public void startNegate() {
	}

	@Override
	public void endNegate() {
	}

	@Override
	public void unknown(RequestPredicate predicate) {
	}

}
