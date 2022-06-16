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

import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;

/**
 * Default implementation of {@link Rendering}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultRendering implements Rendering {

	private static final HttpHeaders EMPTY_HEADERS = HttpHeaders.EMPTY;


	private final Object view;

	private final Map<String, Object> model;

	@Nullable
	private final HttpStatusCode status;

	private final HttpHeaders headers;


	DefaultRendering(Object view, @Nullable Model model, @Nullable HttpStatusCode status, @Nullable HttpHeaders headers) {
		this.view = view;
		this.model = (model != null ? model.asMap() : Collections.emptyMap());
		this.status = status;
		this.headers = (headers != null ? headers : EMPTY_HEADERS);
	}


	@Override
	@Nullable
	public Object view() {
		return this.view;
	}

	@Override
	public Map<String, Object> modelAttributes() {
		return this.model;
	}

	@Override
	@Nullable
	public HttpStatusCode status() {
		return this.status;
	}

	@Override
	public HttpHeaders headers() {
		return this.headers;
	}


	@Override
	public String toString() {
		return "Rendering[view=" + this.view + ", modelAttributes=" + this.model +
				", status=" + this.status + ", headers=" + this.headers + "]";
	}
}
