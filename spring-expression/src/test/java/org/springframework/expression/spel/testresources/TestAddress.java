package org.springframework.expression.spel.testresources;

import java.util.List;

public class TestAddress{
		private String street;
		private List<String> crossStreets;
		
		public String getStreet() {
			return street;
		}
		public void setStreet(String street) {
			this.street = street;
		}
		public List<String> getCrossStreets() {
			return crossStreets;
		}
		public void setCrossStreets(List<String> crossStreets) {
			this.crossStreets = crossStreets;
		}
	}
