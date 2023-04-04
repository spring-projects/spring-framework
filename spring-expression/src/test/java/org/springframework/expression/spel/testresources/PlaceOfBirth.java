/*
 * Copyright 2002-2023 the original author or authors.
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

///CLOVER:OFF
public class PlaceOfBirth {

	private String city;

	public String Country;

	/**
	 * Keith now has a converter that supports String to X, if X has a ctor that takes a String.
	 * In order for round tripping to work we need toString() for X to return what it was
	 * constructed with.  This is a bit of a hack because a PlaceOfBirth also encapsulates a
	 * country - but as it is just a test object, it is ok.
	 */
	@Override
	public String toString() {
		return city;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String s) {
		this.city = s;
	}

	public PlaceOfBirth(String string) {
		this.city=string;
	}

	public int doubleIt(int i) {
		return i*2;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof PlaceOfBirth otherPOB)) {
			return false;
		}
		return (city.equals(otherPOB.city));
	}

	@Override
	public int hashCode() {
		return city.hashCode();
	}

}
