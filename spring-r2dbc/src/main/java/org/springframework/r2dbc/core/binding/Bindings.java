/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.r2dbc.core.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;

import io.r2dbc.spi.Statement;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Value object representing value and {@code null} bindings
 * for a {@link Statement} using {@link BindMarkers}.
 * Bindings are typically immutable.
 *
 * @author Mark Paluch
 * @since 5.3
 */
public class Bindings implements Iterable<Bindings.Binding> {

	private static final Bindings EMPTY = new Bindings();

	private final Map<BindMarker, Binding> bindings;


	/**
	 * Create empty {@link Bindings}.
	 */
	public Bindings() {
		this.bindings = Collections.emptyMap();
	}

	/**
	 * Create {@link Bindings} from the given collection.
	 * @param bindings a collection of {@link Binding} objects
	 */
	public Bindings(Collection<Binding> bindings) {
		Assert.notNull(bindings, "Collection must not be null");
		Map<BindMarker, Binding> mapping = CollectionUtils.newLinkedHashMap(bindings.size());
		bindings.forEach(binding -> mapping.put(binding.getBindMarker(), binding));
		this.bindings = mapping;
	}

	Bindings(Map<BindMarker, Binding> bindings) {
		this.bindings = bindings;
	}


	protected Map<BindMarker, Binding> getBindings() {
		return this.bindings;
	}

	/**
	 * Merge this bindings with an other {@link Bindings} object and
	 * create a new merged {@link Bindings} object.
	 * @param other the object to merge with
	 * @return a newly merged {@link Bindings} object
	 */
	public Bindings and(Bindings other) {
		return merge(this, other);
	}

	/**
	 * Apply the bindings to a {@link BindTarget}.
	 * @param bindTarget the target to apply bindings to
	 */
	public void apply(BindTarget bindTarget) {
		Assert.notNull(bindTarget, "BindTarget must not be null");
		this.bindings.forEach((marker, binding) -> binding.apply(bindTarget));
	}

	/**
	 * Perform the given action for each binding of this {@link Bindings} until all
	 * bindings have been processed or the action throws an exception. Actions are
	 * performed in the order of iteration (if an iteration order is specified).
	 * Exceptions thrown by the action are relayed to the
	 * @param action the action to be performed for each {@link Binding}
	 */
	public void forEach(Consumer<? super Binding> action) {
		this.bindings.forEach((marker, binding) -> action.accept(binding));
	}

	@Override
	public Iterator<Binding> iterator() {
		return this.bindings.values().iterator();
	}

	@Override
	public Spliterator<Binding> spliterator() {
		return this.bindings.values().spliterator();
	}


	/**
	 * Return an empty {@link Bindings} object.
	 */
	public static Bindings empty() {
		return EMPTY;
	}

	/**
	 * Merge this bindings with an other {@link Bindings} object and
	 * create a new merged {@link Bindings} object.
	 * @param left the left object to merge with
	 * @param right the right object to merge with
	 * @return a newly merged {@link Bindings} object
	 */
	public static Bindings merge(Bindings left, Bindings right) {
		Assert.notNull(left, "Left side Bindings must not be null");
		Assert.notNull(right, "Right side Bindings must not be null");
		List<Binding> result = new ArrayList<>(left.getBindings().size() + right.getBindings().size());
		result.addAll(left.getBindings().values());
		result.addAll(right.getBindings().values());
		return new Bindings(result);
	}


	/**
	 * Base class for value objects representing a value or a {@code NULL} binding.
	 */
	public abstract static class Binding {

		private final BindMarker marker;

		protected Binding(BindMarker marker) {
			this.marker = marker;
		}

		/**
		 * Return the associated {@link BindMarker}.
		 */
		public BindMarker getBindMarker() {
			return this.marker;
		}

		/**
		 * Return whether the binding has a value associated with it.
		 * @return {@code true} if there is a value present,
		 * otherwise {@code false} for a {@code NULL} binding
		 */
		public abstract boolean hasValue();

		/**
		 * Return whether the binding is empty.
		 * @return {@code true} if this is is a {@code NULL} binding
		 */
		public boolean isNull() {
			return !hasValue();
		}

		/**
		 * Return the binding value.
		 * @return the value of this binding
		 * (can be {@code null} if this is a {@code NULL} binding)
		 */
		@Nullable
		public abstract Object getValue();

		/**
		 * Apply the binding to a {@link BindTarget}.
		 * @param bindTarget the target to apply bindings to
		 */
		public abstract void apply(BindTarget bindTarget);
	}


	/**
	 * Value binding.
	 */
	static class ValueBinding extends Binding {

		private final Object value;

		ValueBinding(BindMarker marker, Object value) {
			super(marker);
			this.value = value;
		}

		@Override
		public boolean hasValue() {
			return true;
		}

		@Override
		@NonNull
		public Object getValue() {
			return this.value;
		}

		@Override
		public void apply(BindTarget bindTarget) {
			getBindMarker().bind(bindTarget, getValue());
		}
	}


	/**
	 * {@code NULL} binding.
	 */
	static class NullBinding extends Binding {

		private final Class<?> valueType;

		NullBinding(BindMarker marker, Class<?> valueType) {
			super(marker);
			this.valueType = valueType;
		}

		@Override
		public boolean hasValue() {
			return false;
		}

		@Override
		@Nullable
		public Object getValue() {
			return null;
		}

		public Class<?> getValueType() {
			return this.valueType;
		}

		@Override
		public void apply(BindTarget bindTarget) {
			getBindMarker().bindNull(bindTarget, getValueType());
		}
	}

}
