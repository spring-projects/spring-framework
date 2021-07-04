/*
 * Copyright 2002-2016 the original author or authors.
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
 * Concrete subclass of {@link PersistentEntity} representing a <em>person</em>
 * entity; intended for use in unit tests.
 *
 * @author Sam Brannen
 * @since 2.5
 */
public class PersonEntity extends PersistentEntity implements Person {

	protected String name;

	private int age;

	String eyeColor;

	boolean likesPets = false;

	private Number favoriteNumber;


	@Override
	public String getName() {
		return this.name;
	}

	@SuppressWarnings("unused")
	private void setName(final String name) {
		this.name = name;
	}

	@Override
	public int getAge() {
		return this.age;
	}

	protected void setAge(final int age) {
		this.age = age;
	}

	@Override
	public String getEyeColor() {
		return this.eyeColor;
	}

	void setEyeColor(final String eyeColor) {
		this.eyeColor = eyeColor;
	}

	@Override
	public boolean likesPets() {
		return this.likesPets;
	}

	protected void setLikesPets(final boolean likesPets) {
		this.likesPets = likesPets;
	}

	@Override
	public Number getFavoriteNumber() {
		return this.favoriteNumber;
	}

	protected void setFavoriteNumber(Number favoriteNumber) {
		this.favoriteNumber = favoriteNumber;
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringCreator(this)
			.append("id", this.getId())
			.append("name", this.name)
			.append("age", this.age)
			.append("eyeColor", this.eyeColor)
			.append("likesPets", this.likesPets)
			.append("favoriteNumber", this.favoriteNumber)
			.toString();
		// @formatter:on
	}

}
