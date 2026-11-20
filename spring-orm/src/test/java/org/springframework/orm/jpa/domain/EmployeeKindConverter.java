/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmployeeKindConverter implements AttributeConverter<EmployeeKind, String> {

	@Override
	public String convertToDatabaseColumn(EmployeeKind employeeKind) {
		if (employeeKind != null) {
			return employeeKind.getName();
		}
		return null;
	}

	@Override
	public EmployeeKind convertToEntityAttribute(String data) {
		if (data != null) {
			EmployeeKind employeeKind = new EmployeeKind();
			employeeKind.setName(data);
			return employeeKind;
		}
		return null;
	}

}
