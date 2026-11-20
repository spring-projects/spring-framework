/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind.support;

/**
 * Simple implementation of the {@link SessionStatus} interface,
 * keeping the {@code complete} flag as an instance variable.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class SimpleSessionStatus implements SessionStatus {

	private boolean complete = false;


	@Override
	public void setComplete() {
		this.complete = true;
	}

	@Override
	public boolean isComplete() {
		return this.complete;
	}

}
