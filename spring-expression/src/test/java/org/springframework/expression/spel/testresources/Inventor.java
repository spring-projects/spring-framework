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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.util.ObjectUtils;

///CLOVER:OFF
@SuppressWarnings("unused")
public class Inventor {

	private String name;
	public String _name;
	public String _name_;
	public String publicName;
	private PlaceOfBirth placeOfBirth;
	private Date birthdate;
	private int sinNumber;
	private String nationality;
	private String[] inventions;
	public String randomField;
	public Map<String,String> testMap;
	private boolean wonNobelPrize;
	private PlaceOfBirth[] placesLived;
	private List<PlaceOfBirth> placesLivedList = new ArrayList<>();
	public ArrayContainer arrayContainer;
	public boolean publicBoolean;
	private boolean accessedThroughGetSet;
	public List<Integer> listOfInteger = new ArrayList<>();
	public List<Boolean> booleanList = new ArrayList<>();
	public Map<String,Boolean> mapOfStringToBoolean = new LinkedHashMap<>();
	public Map<Integer,String> mapOfNumbersUpToTen = new LinkedHashMap<>();
	public List<Integer> listOfNumbersUpToTen = new ArrayList<>();
	public List<Integer> listOneFive = new ArrayList<>();
	public String[] stringArrayOfThreeItems = new String[]{"1","2","3"};
	private String foo;
	public int counter;

	public Inventor(String name, Date birthdate, String nationality) {
		this.name = name;
		this._name = name;
		this._name_ = name;
		this.birthdate = birthdate;
		this.nationality = nationality;
		this.arrayContainer = new ArrayContainer();
		testMap = new HashMap<>();
		testMap.put("monday", "montag");
		testMap.put("tuesday", "dienstag");
		testMap.put("wednesday", "mittwoch");
		testMap.put("thursday", "donnerstag");
		testMap.put("friday", "freitag");
		testMap.put("saturday", "samstag");
		testMap.put("sunday", "sonntag");
		listOneFive.add(1);
		listOneFive.add(5);
		booleanList.add(false);
		booleanList.add(false);
		listOfNumbersUpToTen.add(1);
		listOfNumbersUpToTen.add(2);
		listOfNumbersUpToTen.add(3);
		listOfNumbersUpToTen.add(4);
		listOfNumbersUpToTen.add(5);
		listOfNumbersUpToTen.add(6);
		listOfNumbersUpToTen.add(7);
		listOfNumbersUpToTen.add(8);
		listOfNumbersUpToTen.add(9);
		listOfNumbersUpToTen.add(10);
		mapOfNumbersUpToTen.put(1,"one");
		mapOfNumbersUpToTen.put(2,"two");
		mapOfNumbersUpToTen.put(3,"three");
		mapOfNumbersUpToTen.put(4,"four");
		mapOfNumbersUpToTen.put(5,"five");
		mapOfNumbersUpToTen.put(6,"six");
		mapOfNumbersUpToTen.put(7,"seven");
		mapOfNumbersUpToTen.put(8,"eight");
		mapOfNumbersUpToTen.put(9,"nine");
		mapOfNumbersUpToTen.put(10,"ten");
	}

	public void setPlaceOfBirth(PlaceOfBirth placeOfBirth2) {
		placeOfBirth = placeOfBirth2;
		this.placesLived = new PlaceOfBirth[] { placeOfBirth2 };
		this.placesLivedList.add(placeOfBirth2);
	}

	public String[] getInventions() {
		return inventions;
	}

	public void setInventions(String... inventions) {
		this.inventions = inventions;
	}

	public PlaceOfBirth getPlaceOfBirth() {
		return placeOfBirth;
	}

	public int throwException(int valueIn) throws Exception {
		counter++;
		switch (valueIn) {
			case 1 -> throw new IllegalArgumentException("IllegalArgumentException for 1");
			case 2 -> throw new RuntimeException("RuntimeException for 2");
			case 4 -> throw new TestException();
		}
		return valueIn;
	}

	@SuppressWarnings("serial")
	public static class TestException extends Exception {}

	public String throwException(PlaceOfBirth pob) {
		return pob.getCity();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean getWonNobelPrize() {
		return wonNobelPrize;
	}

	public void setWonNobelPrize(boolean wonNobelPrize) {
		this.wonNobelPrize = wonNobelPrize;
	}

	public PlaceOfBirth[] getPlacesLived() {
		return placesLived;
	}

	public void setPlacesLived(PlaceOfBirth[] placesLived) {
		this.placesLived = placesLived;
	}

	public List<PlaceOfBirth> getPlacesLivedList() {
		return placesLivedList;
	}

	public void setPlacesLivedList(List<PlaceOfBirth> placesLivedList) {
		this.placesLivedList = placesLivedList;
	}

	public String echo(Object o) {
		return o.toString();
	}

	public String sayHelloTo(String person) {
		return "hello " + person;
	}

	public String printDouble(Double d) {
		return d.toString();
	}

	public String printDoubles(double[] d) {
		return ObjectUtils.nullSafeToString(d);
	}

	public List<String> getDoublesAsStringList() {
		List<String> result = new ArrayList<>();
		result.add("14.35");
		result.add("15.45");
		return result;
	}

	public String joinThreeStrings(String a, String b, String c) {
		return a + b + c;
	}

	public String aVarargsMethod(String... strings) {
		return Arrays.toString(strings);
	}

	public String aVarargsMethod2(int i, String... strings) {
		return i + "-" + Arrays.toString(strings);
	}

	@SuppressWarnings("unchecked")
	public String optionalVarargsMethod(Optional<String>... values) {
		return Arrays.toString(values);
	}

	public String aVarargsMethod3(String str1, String... strings) {
		if (ObjectUtils.isEmpty(strings)) {
			return str1;
		}
		return str1 + "-" + String.join("-", strings);
	}

	public String formatObjectVarargs(String format, Object... args) {
		return String.format(format, args);
	}

	public String formatPrimitiveVarargs(String format, int... nums) {
		Object[] args = new Object[nums.length];
		for (int i = 0; i < nums.length; i++) {
			args[i] = nums[i];
		}
		return String.format(format, args);
	}


	public Inventor(String... strings) {
		if (strings.length > 0) {
			this.name = strings[0];
		}
	}

	public boolean getSomeProperty() {
		return accessedThroughGetSet;
	}

	public void setSomeProperty(boolean b) {
		this.accessedThroughGetSet = b;
	}

	public Date getBirthdate() { return birthdate;}

	public String getFoo() { return foo; }
	public void setFoo(String s) { foo = s; }

	public String getNationality() { return nationality; }
}
