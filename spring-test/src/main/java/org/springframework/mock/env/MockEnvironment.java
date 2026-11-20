/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.mock.env;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Simple {@link ConfigurableEnvironment} implementation exposing
 * {@link #setProperty} and {@link #withProperty} methods for testing purposes.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.2
 * @see MockPropertySource
 */
public class MockEnvironment extends AbstractEnvironment {

	private final MockPropertySource propertySource = new MockPropertySource();


	/**
	 * Create a new {@code MockEnvironment} with a single {@link MockPropertySource}.
	 */
	public MockEnvironment() {
		getPropertySources().addLast(this.propertySource);
	}


	/**
	 * Set a property on the underlying {@link MockPropertySource} for this environment.
	 * @since 6.2.8
	 * @see MockPropertySource#setProperty(String, Object)
	 */
	public void setProperty(String name, Object value) {
		this.propertySource.setProperty(name, value);
	}

	/**
	 * Convenient synonym for {@link #setProperty(String, Object)} that returns
	 * the current instance.
	 * <p>Useful for method chaining and fluent-style use.
	 * @return this {@link MockEnvironment} instance
	 * @since 6.2.8
	 * @see MockPropertySource#withProperty(String, Object)
	 */
	public MockEnvironment withProperty(String name, Object value) {
		setProperty(name, value);
		return this;
	}

}
