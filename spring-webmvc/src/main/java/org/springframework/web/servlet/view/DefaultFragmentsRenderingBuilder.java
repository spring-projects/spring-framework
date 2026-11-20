/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.servlet.ModelAndView;

/**
 * Default {@link FragmentsRendering.Builder} implementation that collects the
 * fragments and creates a {@link DefaultFragmentsRendering}.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
final class DefaultFragmentsRenderingBuilder implements FragmentsRendering.Builder {

	private @Nullable HttpStatusCode status;

	private @Nullable HttpHeaders headers;

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
