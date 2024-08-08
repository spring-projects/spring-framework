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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Container for a model and a view for use with {@link FragmentsRendering} and
 * multi-view rendering. For full page rendering with a single model and view,
 * use {@link Rendering}.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 * @see FragmentsRendering
 */
public final class Fragment {

	@Nullable
	private final String viewName;

	@Nullable
	private final View view;

	@Nullable
	private Map<String, Object> model;


	private Fragment(@Nullable String viewName, @Nullable View view, @Nullable Map<String, Object> model) {
		this.viewName = viewName;
		this.view = view;
		this.model = model;
	}


	/**
	 * Whether this Fragment contains a resolved {@link View} instance.
	 */
	public boolean isResolved() {
		return (this.view != null);
	}

	/**
	 * Return the view name of the Fragment, or {@code null} if not set.
	 */
	@Nullable
	public String viewName() {
		return this.viewName;
	}

	/**
	 * Return the resolved {@link View} instance. This should be called only
	 * after an {@link #isResolved()} check.
	 */
	public View view() {
		Assert.state(this.view != null, "View not resolved");
		return this.view;
	}

	/**
	 * Return the model for this Fragment, or an empty map.
	 */
	public Map<String, Object> model() {
		return (this.model != null ? this.model : Collections.emptyMap());
	}

	/**
	 * Merge attributes from the request model if not already present.
	 */
	public void mergeAttributes(Model model) {
		if (CollectionUtils.isEmpty(model.asMap())) {
			return;
		}
		if (this.model == null) {
			this.model = new LinkedHashMap<>();
		}
		model.asMap().forEach((key, value) -> this.model.putIfAbsent(key, value));
	}


	@Override
	public String toString() {
		return "Fragment [view=" + formatView() + "; model=" + model() + "]";
	}

	private String formatView() {
		return (isResolved() ? "\"" + view() + "\"" : "[" + viewName() + "]");
	}


	/**
	 * Create a Fragment with a view name and a model, also inheriting model
	 * attributes from the top-level model for the request.
	 */
	public static Fragment create(String viewName, Map<String, Object> model) {
		return new Fragment(viewName, null, model);
	}

	/**
	 * Create a Fragment with a view name only, inheriting model attributes from
	 * the top-level model for the request.
	 */
	public static Fragment create(String viewName) {
		return new Fragment(viewName, null, null);
	}

	/**
	 * Variant of {@link #create(String, Map)} with a resolved {@link View}.
	 */
	public static Fragment create(View view, Map<String, Object> model) {
		return new Fragment(null, view, model);
	}

}
