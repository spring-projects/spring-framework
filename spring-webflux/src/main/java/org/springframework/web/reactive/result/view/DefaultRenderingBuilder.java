/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Arrays;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link Rendering.RedirectBuilder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultRenderingBuilder implements Rendering.RedirectBuilder {

	private final Object view;

	@Nullable
	private Model model;

	@Nullable
	private HttpStatusCode status;

	@Nullable
	private HttpHeaders headers;


	DefaultRenderingBuilder(Object view) {
		this.view = view;
	}


	@Override
	public DefaultRenderingBuilder modelAttribute(String name, Object value) {
		initModel().addAttribute(name, value);
		return this;
	}

	@Override
	public DefaultRenderingBuilder modelAttribute(Object value) {
		initModel().addAttribute(value);
		return this;
	}

	@Override
	public DefaultRenderingBuilder modelAttributes(Object... values) {
		initModel().addAllAttributes(Arrays.asList(values));
		return this;
	}

	@Override
	public DefaultRenderingBuilder model(Map<String, ?> map) {
		initModel().addAllAttributes(map);
		return this;
	}

	private Model initModel() {
		if (this.model == null) {
			this.model = new ExtendedModelMap();
		}
		return this.model;
	}

	@Override
	public DefaultRenderingBuilder status(HttpStatusCode status) {
		this.status = status;
		return this;
	}

	@Override
	public DefaultRenderingBuilder header(String headerName, String... headerValues) {
		initHeaders().put(headerName, Arrays.asList(headerValues));
		return this;
	}

	@Override
	public DefaultRenderingBuilder headers(HttpHeaders headers) {
		initHeaders().putAll(headers);
		return this;
	}

	private HttpHeaders initHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
		}
		return this.headers;
	}

	@Override
	public Rendering.RedirectBuilder contextRelative(boolean contextRelative) {
		getRedirectView().setContextRelative(contextRelative);
		return this;
	}

	@Override
	public Rendering.RedirectBuilder propagateQuery(boolean propagate) {
		getRedirectView().setPropagateQuery(propagate);
		return this;
	}

	private RedirectView getRedirectView() {
		Assert.isInstanceOf(RedirectView.class, this.view);
		return (RedirectView) this.view;
	}


	@Override
	public Rendering build() {
		return new DefaultRendering(this.view, this.model, this.status, this.headers);
	}

}
