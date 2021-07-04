/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.factory.BeanDefinitionStoreException;

/**
 * Exception thrown when a bean definition reader encounters an error
 * during the parsing process.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 */
@SuppressWarnings("serial")
public class BeanDefinitionParsingException extends BeanDefinitionStoreException {

	/**
	 * Create a new BeanDefinitionParsingException.
	 * @param problem the configuration problem that was detected during the parsing process
	 */
	public BeanDefinitionParsingException(Problem problem) {
		super(problem.getResourceDescription(), problem.toString(), problem.getRootCause());
	}

}
