/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmployeeLocationConverter implements AttributeConverter<EmployeeLocation, String> {

	@Override
	public String convertToDatabaseColumn(EmployeeLocation employeeLocation) {
		if (employeeLocation != null) {
			return employeeLocation.getLocation();
		}
		return null;
	}

	@Override
	public EmployeeLocation convertToEntityAttribute(String data) {
		if (data != null) {
			EmployeeLocation employeeLocation = new EmployeeLocation();
			employeeLocation.setLocation(data);
			return employeeLocation;
		}
		return null;
	}

}
