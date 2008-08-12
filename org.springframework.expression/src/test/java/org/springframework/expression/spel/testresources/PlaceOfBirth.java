package org.springframework.expression.spel.testresources;

public class PlaceOfBirth {
	private String city;
	
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
	
}