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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;

/**
 * Default implementation of {@link FragmentsRendering.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
class DefaultFragmentsRenderingBuilder implements FragmentsRendering.Builder {

	@Nullable
	private Collection<Fragment> fragmentsCollection;

	@Nullable
	private final Flux<Fragment> fragmentsFlux;

	@Nullable
	private HttpStatusCode status;

	@Nullable
	private HttpHeaders headers;

	DefaultFragmentsRenderingBuilder(Collection<Fragment> fragments) {
		this.fragmentsCollection = new ArrayList<>(fragments);
		this.fragmentsFlux = null;
	}

	DefaultFragmentsRenderingBuilder(Publisher<Fragment> fragments) {
		this.fragmentsFlux = Flux.from(fragments);
	}


	@Override
	public FragmentsRendering.Builder status(HttpStatusCode status) {
		this.status = status;
		return this;
	}

	@Override
	public FragmentsRendering.Builder header(String headerName, String... headerValues) {
		initHeaders().put(headerName, Arrays.asList(headerValues));
		return this;
	}

	@Override
	public FragmentsRendering.Builder headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(initHeaders());
		return this;
	}

	private HttpHeaders initHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
		}
		return this.headers;
	}

	@Override
	public FragmentsRendering.Builder fragment(String viewName, Map<String, Object> model) {
		return fragment(Fragment.create(viewName, model));
	}

	@Override
	public FragmentsRendering.Builder fragment(String viewName) {
		return fragment(Fragment.create(viewName));
	}

	@Override
	public FragmentsRendering.Builder fragment(Fragment fragment) {
		initFragmentsCollection().add(fragment);
		return this;
	}

	private Collection<Fragment> initFragmentsCollection() {
		if (this.fragmentsCollection == null) {
			this.fragmentsCollection = new ArrayList<>();
		}
		return this.fragmentsCollection;
	}

	@Override
	public FragmentsRendering build() {
		return new DefaultFragmentsRendering(
				this.status, (this.headers != null ? this.headers : HttpHeaders.EMPTY), getFragmentsFlux());
	}

	private Flux<Fragment> getFragmentsFlux() {
		if (this.fragmentsFlux != null && this.fragmentsCollection != null) {
			return this.fragmentsFlux.concatWith(Flux.fromIterable(this.fragmentsCollection));
		}
		else if (this.fragmentsFlux != null) {
			return this.fragmentsFlux;
		}
		else if (this.fragmentsCollection != null) {
			return Flux.fromIterable(this.fragmentsCollection);
		}
		else {
			return Flux.empty();
		}
	}


	/**
	 * Default implementation of {@link FragmentsRendering}.
	 */
	private record DefaultFragmentsRendering(@Nullable HttpStatusCode status, HttpHeaders headers, Flux<Fragment> fragments)
			implements FragmentsRendering {
	}

}
