/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc;

/**
 * @author Juergen Hoeller
 */
public class Customer {

	private int id;

	private String forename;


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getForename() {
		return forename;
	}

	public void setForename(String forename) {
		this.forename = forename;
	}


	@Override
	public String toString() {
		return "Customer: id=" + id + "; forename=" + forename;
	}

}
