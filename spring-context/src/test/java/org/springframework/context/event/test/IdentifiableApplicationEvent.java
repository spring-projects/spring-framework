/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event.test;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationEvent;

/**
 * A basic test event that can be uniquely identified easily.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("serial")
public abstract class IdentifiableApplicationEvent extends ApplicationEvent implements Identifiable {

	private final String id;

	protected IdentifiableApplicationEvent(Object source, String id) {
		super(source);
		this.id = id;
	}

	protected IdentifiableApplicationEvent(Object source) {
		this(source, UUID.randomUUID().toString());
	}

	protected IdentifiableApplicationEvent() {
		this(new Object());
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

		IdentifiableApplicationEvent that = (IdentifiableApplicationEvent) o;

		return this.id.equals(that.id);

	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

}
