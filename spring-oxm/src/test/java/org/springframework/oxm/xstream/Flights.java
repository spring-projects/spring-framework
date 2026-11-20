/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.oxm.xstream;

import java.util.ArrayList;
import java.util.List;

public class Flights {

	private List<Flight> flights = new ArrayList<>();

	private List<String> strings = new ArrayList<>();

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
