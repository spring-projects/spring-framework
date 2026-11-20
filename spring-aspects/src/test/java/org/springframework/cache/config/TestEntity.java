/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.config;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * Copy of the shared {@code TestEntity}: necessary
 * due to issues with Gradle test fixtures and AspectJ configuration
 * in the Gradle build.
 *
 * <p>Simple test entity for use with caching tests.
 *
 * @author Michael Plod
 */
public class TestEntity {

	private Long id;

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.id);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this) {
			return true;
		}
		return (obj instanceof TestEntity that && ObjectUtils.nullSafeEquals(this.id, that.id));
	}

}
