/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event.test;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractIdentifiable implements Identifiable {

	private final String id;

	public AbstractIdentifiable() {
		this.id = UUID.randomUUID().toString();
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		AbstractIdentifiable that = (AbstractIdentifiable) o;

		return this.id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

}
