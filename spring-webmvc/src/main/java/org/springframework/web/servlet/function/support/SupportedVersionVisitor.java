/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.function.support;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.web.accept.DefaultApiVersionStrategy;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;

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
	public void resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
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
	public void param(String name, String value) {

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
