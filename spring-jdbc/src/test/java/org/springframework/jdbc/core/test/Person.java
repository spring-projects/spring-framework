/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * @author Thomas Risberg
 */
public class Person {

	private String name;

	private long age;

	private java.util.Date birth_date;

	private BigDecimal balance;


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getAge() {
		return age;
	}

	public void setAge(long age) {
		this.age = age;
	}

	public java.util.Date getBirth_date() {
		return birth_date;
	}

	public void setBirth_date(java.util.Date birth_date) {
		this.birth_date = birth_date;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balanace) {
		this.balance = balanace;
	}

}
