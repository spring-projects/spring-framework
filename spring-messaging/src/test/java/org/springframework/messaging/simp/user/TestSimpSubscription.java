/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.user;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * @author Rossen Stoyanchev
 */
public class TestSimpSubscription implements SimpSubscription {

	private final String destination;

	private final String id;

	private TestSimpSession session;


	public TestSimpSubscription(String id, String destination) {
		this.destination = destination;
		this.id = id;
	}


	@Override
	public String getId() {
		return id;
	}

	@Override
	public TestSimpSession getSession() {
		return this.session;
	}

	public void setSession(TestSimpSession session) {
		this.session = session;
	}

	@Override
	public String getDestination() {
		return destination;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof SimpSubscription that &&
				ObjectUtils.nullSafeEquals(getSession(), that.getSession()) &&
				this.id.equals(that.getId())));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, getSession());
	}

	@Override
	public String toString() {
		return "destination=" + this.destination;
	}

}
