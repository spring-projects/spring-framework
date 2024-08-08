/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.beans.testfixture.beans;

import java.util.Objects;

import org.springframework.lang.Nullable;

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
