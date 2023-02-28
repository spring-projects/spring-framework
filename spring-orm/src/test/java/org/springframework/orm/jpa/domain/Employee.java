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

package org.springframework.orm.jpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PreRemove;

@Entity
@IdClass(EmployeeId.class)
@Convert(converter = EmployeeKindConverter.class, attributeName = "kind")
public class Employee {

	@Id
	@Column
	private String name;

	@Id
	@Column
	private String department;

	private EmployeeLocation location;

	@Convert(converter = EmployeeCategoryConverter.class)
	private EmployeeCategory category;

	private EmployeeKind kind;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public EmployeeLocation getLocation() {
		return location;
	}

	public void setLocation(EmployeeLocation location) {
		this.location = location;
	}

	public EmployeeCategory getCategory() {
		return category;
	}

	public void setCategory(EmployeeCategory category) {
		this.category = category;
	}

	public EmployeeKind getKind() {
		return kind;
	}

	public void setKind(EmployeeKind kind) {
		this.kind = kind;
	}

	@PreRemove
	public void preRemove() {
	}
}
