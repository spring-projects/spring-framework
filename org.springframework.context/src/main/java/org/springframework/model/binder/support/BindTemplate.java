/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.model.binder.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.model.binder.BindingResults;
import org.springframework.model.binder.MissingFieldException;

/**
 * A template that encapsulates the general bulk-binding algorithm.
 * @author Keith Donald
 * @since 3.0
 * @see #setRequiredFields(String[])
 * @see #bind(Map)
 */
public class BindTemplate  {

	private String[] requiredFields;

	/**
	 * Configure the fields for which values must be present in each bind attempt.
	 * @param fieldNames the required field names
	 * @see MissingFieldException
	 */
	public void setRequiredFields(String[] fieldNames) {
		this.requiredFields = fieldNames;
	}

	// implementing Binder

	public BindingResults bind(Map<String, ? extends Object> fieldValues, FieldBinder fieldBinder) {
		checkRequired(fieldValues);
		ArrayListBindingResults results = new ArrayListBindingResults(fieldValues.size());
		for (Map.Entry<String, ? extends Object> fieldValue : fieldValues.entrySet()) {
			results.add(fieldBinder.bind(fieldValue.getKey(), fieldValue.getValue()));
		}
		return results;
	}


	// internal helpers

	private void checkRequired(Map<String, ? extends Object> fieldValues) {
		if (requiredFields == null) {
			return;
		}
		List<String> missingRequired = new ArrayList<String>();
		for (String required : requiredFields) {
			boolean found = false;
			for (String property : fieldValues.keySet()) {
				if (property.equals(required)) {
					found = true;
				}
			}
			if (!found) {
				missingRequired.add(required);
			}
		}
		if (!missingRequired.isEmpty()) {
			throw new MissingFieldException(missingRequired, fieldValues);
		}
	}

}