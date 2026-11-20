/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.testresources;

import org.jspecify.annotations.Nullable;

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
