package org.springframework.oxm.xstream;

import java.util.ArrayList;
import java.util.List;

public class Flights {

	private List<Flight> flights = new ArrayList<Flight>();

	private List<String> strings = new ArrayList<String>();

	public List<Flight> getFlights() {
		return flights;
	}

	public void setFlights(List<Flight> flights) {
		this.flights = flights;
	}

	public List<String> getStrings() {
		return strings;
	}

	public void setStrings(List<String> strings) {
		this.strings = strings;
	}
}
