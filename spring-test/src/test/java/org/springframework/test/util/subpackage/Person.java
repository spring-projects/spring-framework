/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.util.subpackage;

import org.springframework.core.style.ToStringCreator;

/**
 * Concrete subclass of {@link PersistentEntity} representing a <em>person</em>
 * entity; intended for use in unit tests.
 *
 * @author Sam Brannen
 * @since 2.5
 */
public class Person extends PersistentEntity {

	protected String name;

	private int age;

	String eyeColor;

	boolean likesPets = false;

	private Number favoriteNumber;


	public final String getName() {
		return this.name;
	}

	@SuppressWarnings("unused")
	private final void setName(final String name) {
		this.name = name;
	}

	public final int getAge() {
		return this.age;
	}

	protected final void setAge(final int age) {
		this.age = age;
	}

	public final String getEyeColor() {
		return this.eyeColor;
	}

	final void setEyeColor(final String eyeColor) {
		this.eyeColor = eyeColor;
	}

	public final boolean likesPets() {
		return this.likesPets;
	}

	protected final void setLikesPets(final boolean likesPets) {
		this.likesPets = likesPets;
	}

	public final Number getFavoriteNumber() {
		return this.favoriteNumber;
	}

	protected final void setFavoriteNumber(Number favoriteNumber) {
		this.favoriteNumber = favoriteNumber;
	}

	public String toString() {
		return new ToStringCreator(this)

		.append("id", this.getId())

		.append("name", this.name)

		.append("age", this.age)

		.append("eyeColor", this.eyeColor)

		.append("likesPets", this.likesPets)

		.append("favoriteNumber", this.favoriteNumber)

		.toString();
	}
}
