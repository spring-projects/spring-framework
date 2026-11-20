/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.security;

import java.security.Principal;

import org.jspecify.annotations.Nullable;

/**
 * An implementation of {@link Principal} for testing.
 *
 * @author Rossen Stoyanchev
 */
public class TestPrincipal implements Principal {

	private final String name;

	public TestPrincipal(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TestPrincipal p)) {
			return false;
		}
		return this.name.equals(p.name);
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

}
