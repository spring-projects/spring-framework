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
package org.springframework.ui.lifecycle;

import org.springframework.ui.binding.BindingResults;

/**
 * Decides if validation should run for an execution of the bind and validate lifecycle.
 * @author Keith Donald
 * @since 3.0
 * @see BindAndValidateLifecycle#execute(java.util.Map)
 */
interface ValidationDecider {

	/**
	 * Should validation execute after model binding?
	 * @param results the results of model binding
	 * @return yes or no
	 */
	boolean shouldValidateAfter(BindingResults results);

	/**
	 * Singleton reference to a ValidationDecider that always returns true.
	 */
	static final ValidationDecider ALWAYS_VALIDATE = new ValidationDecider() {
		public boolean shouldValidateAfter(BindingResults results) {
			return true;
		}
	};
}