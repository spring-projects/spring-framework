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

import java.util.LinkedHashMap;

import io.r2dbc.spi.Statement;

import org.springframework.util.Assert;

/**
 * Mutable extension to {@link Bindings} for Value and {@code null} bindings
 * for a {@link Statement} using {@link BindMarkers}.
 *
 * @author Mark Paluch
 * @since 5.3
 */
public class MutableBindings extends Bindings {

	private final BindMarkers markers;


	/**
	 * Create new {@link MutableBindings}.
	 * @param markers must not be {@code null}.
	 */
	public MutableBindings(BindMarkers markers) {
		super(new LinkedHashMap<>());
		Assert.notNull(markers, "BindMarkers must not be null");
		this.markers = markers;
	}


	/**
	 * Obtain the next {@link BindMarker}.
	 * Increments {@link BindMarkers} state
	 * @return the next {@link BindMarker}
	 */
	public BindMarker nextMarker() {
		return this.markers.next();
	}

	/**
	 * Obtain the next {@link BindMarker} with a name {@code hint}.
	 * Increments {@link BindMarkers} state.
	 * @param hint name hint
	 * @return the next {@link BindMarker}
	 */
	public BindMarker nextMarker(String hint) {
		return this.markers.next(hint);
	}

	/**
	 * Bind a value to {@link BindMarker}.
	 * @param marker must not be {@code null}
	 * @param value must not be {@code null}
	 */
	public MutableBindings bind(BindMarker marker, Object value) {
		Assert.notNull(marker, "BindMarker must not be null");
		Assert.notNull(value, "Value must not be null");
		getBindings().put(marker, new ValueBinding(marker, value));
		return this;
	}

	/**
	 * Bind a value and return the related {@link BindMarker}.
	 * Increments {@link BindMarkers} state.
	 * @param value must not be {@code null}
	 */
	public BindMarker bind(Object value) {
		Assert.notNull(value, "Value must not be null");
		BindMarker marker = nextMarker();
		getBindings().put(marker, new ValueBinding(marker, value));
		return marker;
	}

	/**
	 * Bind a {@code NULL} value to {@link BindMarker}.
	 * @param marker must not be {@code null}
	 * @param valueType must not be {@code null}
	 */
	public MutableBindings bindNull(BindMarker marker, Class<?> valueType) {
		Assert.notNull(marker, "BindMarker must not be null");
		Assert.notNull(valueType, "Value type must not be null");
		getBindings().put(marker, new NullBinding(marker, valueType));
		return this;
	}

	/**
	 * Bind a {@code NULL} value and return the related {@link BindMarker}.
	 * Increments {@link BindMarkers} state.
	 * @param valueType must not be {@code null}
	 */
	public BindMarker bindNull(Class<?> valueType) {
		Assert.notNull(valueType, "Value type must not be null");
		BindMarker marker = nextMarker();
		getBindings().put(marker, new NullBinding(marker, valueType));
		return marker;
	}

}
