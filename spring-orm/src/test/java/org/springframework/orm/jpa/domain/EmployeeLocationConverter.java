/*
 * Copyright 2002-2022 the original author or authors.
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
