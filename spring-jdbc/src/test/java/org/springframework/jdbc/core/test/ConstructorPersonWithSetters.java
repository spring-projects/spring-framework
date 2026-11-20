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
public class ConstructorPersonWithSetters {

	private String name;

	private long age;

	private Date birthDate;

	private BigDecimal balance;


	public ConstructorPersonWithSetters(String name, long age, BigDecimal balance) {
		this.name = name.toUpperCase();
		this.age = age;
		this.balance = balance;
	}


	public void setName(String name) {
		throw new UnsupportedOperationException();
	}

	public void setAge(long age) {
		throw new UnsupportedOperationException();
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public void setBalance(BigDecimal balance) {
		throw new UnsupportedOperationException();
	}

	public String name() {
		return this.name;
	}

	public long age() {
		return this.age;
	}

	public Date birthDate() {
		return this.birthDate;
	}

	public BigDecimal balance() {
		return this.balance;
	}

}
