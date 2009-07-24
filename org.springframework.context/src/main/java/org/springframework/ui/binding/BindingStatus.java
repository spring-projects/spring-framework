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
 * FieldModel binding states.
 * @author Keith Donald
 * @since 3.0
 * @see FieldModel#getBindingStatus()
 */
public enum BindingStatus {
	
	/**
	 * Initial state: No value is buffered, and there is a direct channel to the model value.
	 */
	CLEAN,
	
	/**
	 * An invalid submitted value is applied.
	 */
	INVALID_SUBMITTED_VALUE,
	
	/**
	 * The binding buffer contains a valid value that has not been committed.
	 */
	DIRTY,

	/**
	 * The buffered value has been committed.
	 */
	COMMITTED,
	
	/**
	 * The buffered value failed to commit.
	 */
	COMMIT_FAILURE
}