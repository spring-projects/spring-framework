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
package org.springframework.ui.binding;

/**
 * The result of a bind operation.
 * @author Keith Donald
 * @see Binder#bind(java.util.List)
 * @see Binding#setValue(Object)
 */
public interface BindingResult {

	/**
	 * The name of the model property associated with this binding result.
	 */
	String getProperty();
	
	/**
	 * Indicates if this result is an error result.
	 */
	boolean isError();

	/**
	 * If an error result, the error code; for example, "invalidFormat", "propertyNotFound", or "evaluationException".
	 */
	String getErrorCode();

	/**
	 * If an error result, the cause of the error.
	 * @return the cause, or <code>null</code> if this is not an error
	 */
	Throwable getErrorCause();

	/**
	 * The raw user-entered value for which binding was attempted.
	 * If not an error result, this value was successfully bound to the model.
	 */
	Object getUserValue();

}