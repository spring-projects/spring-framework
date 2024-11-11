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

package org.springframework.expression.spel.testresources;

import org.springframework.lang.Nullable;

public class PlaceOfBirth {

	private String city;

	public String Country;


	public PlaceOfBirth(String city) {
		this.city = city;
	}


	public String getCity() {
		return this.city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public int doubleIt(int i) {
		return i * 2;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		return (o instanceof PlaceOfBirth that && this.city.equals(that.city));
	}

	@Override
	public int hashCode() {
		return this.city.hashCode();
	}

	/**
	 * ObjectToObjectConverter supports String to X conversions, if X has a
	 * constructor that takes a String.
	 * <p>In order for round-tripping to work, we need toString() for PlaceOfBirth
	 * to return what it was constructed with. This is a bit of a hack, because a
	 * PlaceOfBirth also encapsulates a country, but as it is just a test object,
	 * it is OK.
	 */
	@Override
	public String toString() {
		return this.city;
	}

}
