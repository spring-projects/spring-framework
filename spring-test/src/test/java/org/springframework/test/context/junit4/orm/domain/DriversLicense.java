/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.test.context.junit4.orm.domain;

/**
 * DriversLicense POJO.
 *
 * @author Sam Brannen
 * @since 3.0
 */
public class DriversLicense {

	private Long id;

	private Long number;


	public DriversLicense() {
	}

	public DriversLicense(Long number) {
		this(null, number);
	}

	public DriversLicense(Long id, Long number) {
		this.id = id;
		this.number = number;
	}

	public Long getId() {
		return this.id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	public Long getNumber() {
		return this.number;
	}

	public void setNumber(Long number) {
		this.number = number;
	}

}
