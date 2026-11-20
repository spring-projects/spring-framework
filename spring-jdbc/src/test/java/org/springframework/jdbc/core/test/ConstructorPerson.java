/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.test;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Juergen Hoeller
 */
public class ConstructorPerson {

	private final String name;

	private final long age;

	private final Date birth_date;

	private final BigDecimal balance;


	public ConstructorPerson(String name, long age, Date birth_date, BigDecimal balance) {
		this.name = name;
		this.age = age;
		this.birth_date = birth_date;
		this.balance = balance;
	}


	public String name() {
		return this.name;
	}

	public long age() {
		return this.age;
	}

	public Date birth_date() {
		return this.birth_date;
	}

	public BigDecimal balance() {
		return this.balance;
	}

}
