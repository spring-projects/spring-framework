package org.springframework.expression.spel.testresources;

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
	public String toString() {return city;}

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
	public boolean equals(Object o) {
		if (!(o instanceof PlaceOfBirth)) {
			return false;
		}
		PlaceOfBirth oPOB = (PlaceOfBirth)o;
		return (city.equals(oPOB.city));
	}

	@Override
	public int hashCode() {
		return city.hashCode();
	}

}
