/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * @author Rob Harrop
 * @since 2.0
 */
public class Pet {

	private String name;

	public Pet(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return (this == obj) ||
				(obj instanceof Pet that && Objects.equals(this.name, that.name));
	}

	@Override
	public int hashCode() {
		return (this.name != null ? this.name.hashCode() : 0);
	}

}
