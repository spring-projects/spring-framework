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
 * TODO Document Validator.
 *
 * @author Keith Donald
 * @since 3.0
 */
public interface Validator {

	/**
	 * TODO Document validate().
	 *
	 * @param model
	 * @param properties
	 * @return
	 */
	ValidationResults validate(Object model, List<String> properties);

}
