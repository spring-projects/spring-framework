/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
