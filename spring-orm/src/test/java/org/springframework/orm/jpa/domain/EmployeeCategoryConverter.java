/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EmployeeCategoryConverter implements AttributeConverter<EmployeeCategory, String> {

	@Override
	public String convertToDatabaseColumn(EmployeeCategory employeeCategory) {
		if (employeeCategory != null) {
			return employeeCategory.getName();
		}
		return null;
	}

	@Override
	public EmployeeCategory convertToEntityAttribute(String data) {
		if (data != null) {
			EmployeeCategory employeeCategory = new EmployeeCategory();
			employeeCategory.setName(data);
			return employeeCategory;
		}
		return null;
	}

}
