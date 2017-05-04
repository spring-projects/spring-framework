/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.http.HttpStatus;
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

	private Model model;

	private HttpStatus status;

	private HttpHeaders headers;


	DefaultRenderingBuilder(Object view) {
		this.view = view;
	}


	@Override
	public DefaultRenderingBuilder modelAttribute(String name, Object value) {
		initModel();
		this.model.addAttribute(name, value);
		return this;
	}

	private void initModel() {
		if (this.model == null) {
			this.model = new ExtendedModelMap();
		}
	}

	@Override
	public DefaultRenderingBuilder modelAttribute(Object value) {
		initModel();
		this.model.addAttribute(value);
		return this;
	}

	@Override
	public DefaultRenderingBuilder modelAttributes(Object... values) {
		initModel();
		this.model.addAllAttributes(Arrays.asList(values));
		return this;
	}

	@Override
	public DefaultRenderingBuilder model(Map<String, ?> map) {
		initModel();
		this.model.addAllAttributes(map);
		return this;
	}

	@Override
	public DefaultRenderingBuilder status(HttpStatus status) {
		this.status = status;
		return this;
	}

	@Override
	public DefaultRenderingBuilder header(String headerName, String... headerValues) {
		initHeaders();
		this.headers.put(headerName, Arrays.asList(headerValues));
		return this;
	}

	@Override
	public DefaultRenderingBuilder headers(HttpHeaders headers) {
		initHeaders();
		this.headers.putAll(headers);
		return this;
	}

	private void initHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
		}
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
