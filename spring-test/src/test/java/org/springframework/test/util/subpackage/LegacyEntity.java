/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.util.subpackage;

import org.springframework.core.style.ToStringCreator;

/**
 * A <em>legacy entity</em> whose {@link #toString()} method has side effects;
 * intended for use in unit tests.
 *
 * @author Sam Brannen
 * @since 3.2
 */
public class LegacyEntity {

	private Object collaborator = new Object() {

		@Override
		public String toString() {
			throw new LegacyEntityException(
				"Invoking toString() on the default collaborator causes an undesirable side effect");
		}
	};

	private Integer number;
	private String text;


	public void configure(Integer number, String text) {
		this.number = number;
		this.text = text;
	}

	public Integer getNumber() {
		return this.number;
	}

	public String getText() {
		return this.text;
	}

	public Object getCollaborator() {
		return this.collaborator;
	}

	public void setCollaborator(Object collaborator) {
		this.collaborator = collaborator;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)//
				.append("collaborator", this.collaborator)//
				.toString();
	}

}
