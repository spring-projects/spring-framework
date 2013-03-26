/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.oxm;

/**
 * Base class for exception thrown when a marshalling or unmarshalling error occurs.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see MarshallingFailureException
 * @see UnmarshallingFailureException
 */
@SuppressWarnings("serial")
public abstract class MarshallingException extends XmlMappingException {

	/**
	 * Construct a {@code MarshallingException} with the specified detail message.
	 * @param msg the detail message
	 */
	protected MarshallingException(String msg) {
		super(msg);
	}

	/**
	 * Construct a {@code MarshallingException} with the specified detail message
	 * and nested exception.
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	protected MarshallingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
