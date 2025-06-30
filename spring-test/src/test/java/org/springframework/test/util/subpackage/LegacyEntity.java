/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
