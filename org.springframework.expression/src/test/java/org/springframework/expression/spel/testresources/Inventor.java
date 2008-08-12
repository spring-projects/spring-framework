package org.springframework.expression.spel.testresources;

import java.util.Date;

@SuppressWarnings("unused")
public class Inventor {
	private String name;
	private PlaceOfBirth placeOfBirth;
	Date birthdate;
	private int sinNumber;
	private String nationality;
	private String[] inventions;
	public String randomField;

	public Inventor(String name, Date birthdate, String nationality) {
		this.name = name;
		this.birthdate = birthdate;
		this.nationality = nationality;
	}

	public void setPlaceOfBirth(PlaceOfBirth placeOfBirth2) {
		this.placeOfBirth = placeOfBirth2;
	}

	public void setInventions(String[] inventions) {
		this.inventions = inventions;
	}

	public PlaceOfBirth getPlaceOfBirth() {
		return placeOfBirth;
	}

	public String getName() {
		return name;
	}

	public String echo(Object o) {
		return o.toString();
	}

	public String sayHelloTo(String person) {
		return "hello " + person;
	}

	public String joinThreeStrings(String a, String b, String c) {
		return a + b + c;
	}
	
	public int aVarargsMethod(String...strings ) {
		if (strings==null) return 0;
		return strings.length;
	}
	public int aVarargsMethod2(int i, String...strings ) {
		if (strings==null) return i;
		return strings.length+i;
	}

	public Inventor(String...strings ) {
		
	}
}
