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

package org.springframework.oxm.castor;

import org.springframework.oxm.XmlMappingException;

/**
 * Exception thrown by {@link CastorMarshaller} whenever it encounters a mapping problem.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class CastorMappingException extends XmlMappingException {

	/**
	 * Construct a <code>CastorMappingException</code> with the specified detail message
	 * and nested exception.
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	public CastorMappingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
