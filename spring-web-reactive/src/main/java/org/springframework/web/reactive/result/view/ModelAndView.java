/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.Conventions;
import org.springframework.util.Assert;

/**
 * Immutable holder for both model and view in the web MVC framework. Note that these are entirely
 * distinct. This class merely holds both to make it possible for a handler to return both model
 * and view in a single return value.
 *
 * <p>Represents a model and view returned by a handler. The view can take the form of a String
 * view name which will need to be resolved by a {@link ViewResolver}; alternatively a
 * {@link View} can be specified directly. The model
 * is a {@link Map}, allowing the use of multiple objects keyed by name.
 * @param <T> the type of view the model contains (either a {@link View} or a {@code String}
 * denoting the view name).
 * @author Arjen Poutsma
 * @see ViewResolver
 * @since 5.0
 */
public class ModelAndView<T> {

	/**
	 * View instance or view name String
	 */
	private final T view;

	/**
	 * Model Map
	 */
	private final Map<String, Object> model;


	private ModelAndView(T view, Map<String, Object> model) {
		Assert.notNull(view, "'view' must not be null");
		Assert.notNull(model, "'model' must not be null");
		this.view = view;
		this.model = Collections.unmodifiableMap(model);
	}

	/**
	 * Return a builder for a {@code ModelAndView} with a view name.
	 * @return the builder
	 */
	public static Builder<String> viewName(String viewName) {
		Assert.hasLength(viewName, "'viewName' must not be empty");
		return new BuilderImpl<>(viewName);
	}

	/**
	 * Return a builder for a {@code ModelAndView} with a {@link View} instance.
	 * @return the builder
	 */
	public static Builder<View> view(View view) {
		Assert.notNull(view, "'view' must not be null");
		return new BuilderImpl<>(view);
	}

	/**
	 * Return the optional view name to be resolved by the DispatcherHandler
	 * via a ViewResolver.
	 */
	public Optional<String> viewName() {
		return this.view instanceof String ? Optional.of((String) this.view) : Optional.empty();
	}

	/**
	 * Return the optional View object.
	 */
	public Optional<View> view() {
		return this.view instanceof View ? Optional.of((View) this.view) : Optional.empty();
	}

	/**
	 * Return whether we use a view reference, i.e. {@code true}
	 * if the view has been specified via a name to be resolved by a ViewResolver.
	 */
	public boolean isReference() {
		return (this.view instanceof String);
	}

	/**
	 * Return the unmodifiable model map. Never returns {@code null}.
	 */
	public Map<String, Object> model() {
		return this.model;
	}

	/**
	 * Return diagnostic information about this model and view.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ModelAndView: ");
		if (isReference()) {
			sb.append("reference to view with name '").append(this.view).append("'");
		}
		else {
			sb.append("materialized View is [").append(this.view).append(']');
		}
		sb.append("; model is ").append(this.model);
		return sb.toString();
	}


	/**
	 * A mutable builder for a {@link ModelAndView}.
	 * @param <T> the type of view the model contains (either {@link View} or a view name).
	 */
	public interface Builder<T> {

		/**
		 * Add the supplied model attribute under the supplied name.
		 * @param attributeName the name of the model attribute (never {@code null})
		 * @param attributeValue the model attribute value (can be {@code null})
		 */
		Builder<T> modelAttribute(String attributeName, Object attributeValue);

		/**
		 * Add a model attribute using parameter name generation.
		 * @param attributeValue the object to add to the model (never {@code null})
		 */
		Builder<T> modelAttribute(Object attributeValue);

		/**
		 * Copy all attributes in the supplied {@code Map} into the model.
		 * @see #modelAttribute(String, Object)
		 */
		Builder<T> modelAttributes(Map<String, ?> attributes);

		/**
		 * Builds the {@link ModelAndView}.
		 * @return the built model and view
		 */
		ModelAndView<T> build();

	}


	private static class BuilderImpl<T> implements Builder<T> {

		private final T view;

		private final Map<String, Object> model = new LinkedHashMap<>();

		public BuilderImpl(T view) {
			this.view = view;
		}

		@Override
		public Builder<T> modelAttribute(String attributeName, Object attributeValue) {
			Assert.notNull(attributeName, "Model attribute name must not be null");
			this.model.put(attributeName, attributeValue);
			return this;
		}

		@Override
		public Builder<T> modelAttribute(Object attributeValue) {
			Assert.notNull(attributeValue, "Model object must not be null");
			if (attributeValue instanceof Collection && ((Collection<?>) attributeValue).isEmpty()) {
				return this;
			}
			return modelAttribute(Conventions.getVariableName(attributeValue), attributeValue);
		}

		@Override
		public Builder<T> modelAttributes(Map<String, ?> attributes) {
			if (attributes != null) {
				this.model.putAll(attributes);
			}
			return this;
		}

		@Override
		public ModelAndView<T> build() {
			return new ModelAndView<T>(this.view, this.model);
		}
	}

}
