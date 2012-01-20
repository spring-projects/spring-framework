/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditorSupport;

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

	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ItemPet)) {
			return false;
		}
		ItemPet otherPet = (ItemPet) other;
		return (this.name != null && this.name.equals(otherPet.getName()));
	}

	public int hashCode() {
		return this.name.hashCode();
	}


	public static class CustomEditor extends PropertyEditorSupport {

		public void setAsText(String text) throws IllegalArgumentException {
			setValue(new ItemPet(text));
		}

		public String getAsText() {
			return ((ItemPet) getValue()).getName();
		}
	}

}
