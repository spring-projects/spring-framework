/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jdbc.core.test;

import java.math.BigDecimal;

/**
 * @author Giuseppe Milicia
 */
public class AddPropertyPerson {

	private String nickname;

	private long year;

	private java.util.Date dateOfBirth;

	private BigDecimal balance;

	public AddPropertyPerson() {
	}

	public String getNickname() {
		return nickname;
	}

	@AddPropertyColumn(name = "name")
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public long getYear() {
		return year;
	}

	@AddPropertyColumn(name = "age")
	public void setYear(long year) {
		this.year = year;
	}

	public java.util.Date getDateOfBirth() {
		return dateOfBirth;
	}

	@AddPropertyColumn(name = "birth_date")
	public void setDateOfBirth(java.util.Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	@AddPropertyColumn(name = "balance")
	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

}
