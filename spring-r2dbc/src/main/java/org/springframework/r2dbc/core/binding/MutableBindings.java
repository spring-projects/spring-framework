/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	 * @param markers the {@link BindMarkers} to wrap
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
	 * @param marker the {@link BindMarker} to bind to
	 * @param value the value to bind
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
	 * @param value the value to bind
	 */
	public BindMarker bind(Object value) {
		Assert.notNull(value, "Value must not be null");
		BindMarker marker = nextMarker();
		getBindings().put(marker, new ValueBinding(marker, value));
		return marker;
	}

	/**
	 * Bind a {@code NULL} value to {@link BindMarker}.
	 * @param marker the {@link BindMarker} to bind to
	 * @param valueType the value type
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
	 * @param valueType the value type
	 */
	public BindMarker bindNull(Class<?> valueType) {
		Assert.notNull(valueType, "Value type must not be null");
		BindMarker marker = nextMarker();
		getBindings().put(marker, new NullBinding(marker, valueType));
		return marker;
	}

}
