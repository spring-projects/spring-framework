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
package org.springframework.ui.validation;

import java.util.List;

/**
 * Validates a model object.
 * @author Keith Donald
 * @param <M> the type of model object this validator supports
 */
public interface Validator {
	
	/**
	 * Validate the properties of the model object.
	 * @param model the model object
	 * @param properties the properties to validate
	 * @return a list of validation failures, empty if there were no failures
	 */
	List<ValidationFailure> validate(Object model, List<String> properties);
}
