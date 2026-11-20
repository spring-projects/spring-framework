/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.testresources;

public class RecordPerson {

	private String name;

	private Company company;

	public RecordPerson(String name) {
		this.name = name;
	}

	public RecordPerson(String name, Company company) {
		this.name = name;
		this.company = company;
	}

	public String name() {
		return name;
	}

	public Company company() {
		return company;
	}

}
