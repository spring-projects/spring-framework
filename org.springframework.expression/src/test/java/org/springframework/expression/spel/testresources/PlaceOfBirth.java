package org.springframework.expression.spel.testresources;

///CLOVER:OFF
public class PlaceOfBirth {
	private String city;
	
	public String Country;
	
	@Override
	public String toString() {return "PlaceOfBirth("+city+")";}

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
	
	public boolean equals(Object o) {
		if (!(o instanceof PlaceOfBirth)) {
			return false;
		}
		PlaceOfBirth oPOB = (PlaceOfBirth)o;
		return (city.equals(oPOB.city));
	}
	
	public int hashCode() {
		return city.hashCode();
	}
	
}