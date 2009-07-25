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
package org.springframework.model.binder;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown by a Binder when a required source value is missing unexpectedly from the sourceValues map.
 * Indicates a client configuration error. 
 * @author Keith Donald
 * @since 3.0
 * @see Binder#bind(java.util.Map)
 */
@SuppressWarnings("serial")
public class MissingFieldException extends RuntimeException {

	private List<String> missing;

	/**
	 * Creates a new missing source values exeption.
	 * @param missing
	 * @param sourceValues
	 */
	public MissingFieldException(List<String> missing, Map<String, ? extends Object> sourceValues) {
		super(getMessage(missing, sourceValues));
		this.missing = missing;
	}

	/**
	 * The property paths for which source values were missing.
	 */
	public List<String> getMissing() {
		return missing;
	}

	private static String getMessage(List<String> missingRequired, Map<String, ? extends Object> sourceValues) {
		if (missingRequired.size() == 1) {
			return "Missing a field [" + missingRequired.get(0) + "]; fieldValues map contained " + sourceValues.keySet();
		} else {
			return "Missing fields " + missingRequired + "; fieldValues map contained " + sourceValues.keySet();
		}
	}

}
