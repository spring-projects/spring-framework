/*
 * Copyright 2002-2020 the original author or authors.
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
