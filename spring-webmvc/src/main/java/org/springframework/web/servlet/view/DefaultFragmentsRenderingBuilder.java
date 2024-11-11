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

package org.springframework.web.servlet.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;

/**
 * Default {@link FragmentsRendering.Builder} implementation that collects the
 * fragments and creates a {@link DefaultFragmentsRendering}.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
final class DefaultFragmentsRenderingBuilder implements FragmentsRendering.Builder {

	@Nullable
	private HttpStatusCode status;

	@Nullable
	private HttpHeaders headers;

	private final Collection<ModelAndView> fragments = new ArrayList<>();


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
	public DefaultFragmentsRenderingBuilder fragment(String viewName, Map<String, Object> model) {
		return fragment(new ModelAndView(viewName, model));
	}

	@Override
	public DefaultFragmentsRenderingBuilder fragment(String viewName) {
		return fragment(new ModelAndView(viewName));
	}

	@Override
	public DefaultFragmentsRenderingBuilder fragment(ModelAndView fragment) {
		this.fragments.add(fragment);
		return this;
	}

	@Override
	public DefaultFragmentsRenderingBuilder fragments(Collection<ModelAndView> fragments) {
		this.fragments.addAll(fragments);
		return this;
	}

	@Override
	public FragmentsRendering build() {
		return new DefaultFragmentsRendering(
				this.status, (this.headers != null ? this.headers : HttpHeaders.EMPTY), this.fragments);
	}

}
