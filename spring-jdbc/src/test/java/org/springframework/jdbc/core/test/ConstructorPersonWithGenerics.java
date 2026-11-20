/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author Juergen Hoeller
 */
public class ConstructorPersonWithGenerics {

	private final String name;

	private final long age;

	private final Date bd;

	private final List<BigDecimal> balance;


	public ConstructorPersonWithGenerics(String name, long age, Date birthDate, List<BigDecimal> balance) {
		this.name = name;
		this.age = age;
		this.bd = birthDate;
		this.balance = balance;
	}


	public String name() {
		return this.name;
	}

	public long age() {
		return this.age;
	}

	public Date birthDate() {
		return this.bd;
	}

	public List<BigDecimal> balance() {
		return this.balance;
	}

}
