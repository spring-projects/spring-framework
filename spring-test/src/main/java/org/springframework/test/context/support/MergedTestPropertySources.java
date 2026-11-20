/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.support.PropertySourceDescriptor;
import org.springframework.core.style.DefaultToStringStyler;
import org.springframework.core.style.SimpleValueStyler;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;

/**
 * {@code MergedTestPropertySources} encapsulates the <em>merged</em> property
 * sources declared on a test class and all of its superclasses and enclosing
 * classes via {@link TestPropertySource @TestPropertySource}.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see TestPropertySource
 */
class MergedTestPropertySources {

	private static final MergedTestPropertySources empty = new MergedTestPropertySources(List.of(), new String[0]);

	private final List<PropertySourceDescriptor> descriptors;

	private final String[] properties;


	/**
	 * Factory for an <em>empty</em> {@code MergedTestPropertySources} instance.
	 */
	static MergedTestPropertySources empty() {
		return empty;
	}


	/**
	 * Create a {@code MergedTestPropertySources} instance with the supplied
	 * {@code descriptors} and {@code properties}.
	 * @param descriptors the descriptors for resource locations
	 * of properties files; may be empty but never {@code null}
	 * @param properties the properties in the form of {@code key=value} pairs;
	 * may be empty but never {@code null}
	 */
	MergedTestPropertySources(List<PropertySourceDescriptor> descriptors, String[] properties) {
		Assert.notNull(descriptors, "The descriptors list must not be null");
		Assert.notNull(properties, "The properties array must not be null");
		this.descriptors = descriptors;
		this.properties = properties;
	}

	/**
	 * Get the descriptors for resource locations of properties files.
	 * @see TestPropertySource#locations
	 * @see TestPropertySource#encoding
	 * @see TestPropertySource#factory
	 */
	List<PropertySourceDescriptor> getPropertySourceDescriptors() {
		return this.descriptors;
	}

	/**
	 * Get the properties in the form of <em>key-value</em> pairs.
	 * @see TestPropertySource#properties()
	 */
	String[] getProperties() {
		return this.properties;
	}

	/**
	 * Determine if the supplied object is equal to this {@code MergedTestPropertySources}
	 * instance by comparing both objects' {@linkplain #getPropertySourceDescriptors()
	 * descriptors} and {@linkplain #getProperties() properties}.
	 * @since 5.3
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}

		MergedTestPropertySources that = (MergedTestPropertySources) other;
		if (!this.descriptors.equals(that.descriptors)) {
			return false;
		}
		if (!Arrays.equals(this.properties, that.properties)) {
			return false;
		}

		return true;
	}

	/**
	 * Generate a unique hash code for all properties of this
	 * {@code MergedTestPropertySources} instance.
	 * @since 5.3
	 */
	@Override
	public int hashCode() {
		int result = this.descriptors.hashCode();
		result = 31 * result + Arrays.hashCode(this.properties);
		return result;
	}

	/**
	 * Provide a String representation of this {@code MergedTestPropertySources}
	 * instance.
	 * @since 5.3
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this, new DefaultToStringStyler(new SimpleValueStyler()))
				.append("descriptors", this.descriptors)
				.append("properties", this.properties)
				.toString();
	}

}
