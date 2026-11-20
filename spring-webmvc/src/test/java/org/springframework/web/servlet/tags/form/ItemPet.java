/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditorSupport;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * @author Juergen Hoeller
 */
public class ItemPet {

	private String name;

	public ItemPet(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLabel() {
		return this.name.toUpperCase();
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ItemPet that &&
				ObjectUtils.nullSafeEquals(this.name, that.getName())));
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}


	public static class CustomEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			setValue(new ItemPet(text));
		}

		@Override
		public String getAsText() {
			return ((ItemPet) getValue()).getName();
		}
	}

}
