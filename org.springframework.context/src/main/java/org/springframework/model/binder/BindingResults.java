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

import org.springframework.context.alert.Severity;

/**
 * The results of a bind operation.
 * @author Keith Donald
 * @since 3.0
 * @see Binder#bind(java.util.Map, Object)
 */
public interface BindingResults extends Iterable<BindingResult> {

	/**
	 * The subset of BindingResults that were successful.
	 */
	List<BindingResult> successes();

	/**
	 * The subset of BindingResults that failed.
	 */
	List<BindingResult> failures();
	
	/**
	 * If there is at least one failure with a Severity equal to or greater than {@link Severity#ERROR}.
	 * @see BindingResults#failures()
	 */
	boolean hasErrors();
	
	/**
	 * The subset of BindingResults that failed with {@link Severity#ERROR} or greater.
	 */
	List<BindingResult> errors();
	
	/**
	 * The total number of results.
	 */
	int size();

	/**
	 * The BindingResult at the specified index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds
	 */
	BindingResult get(int index);

	/**
	 * The BindingResult for the specified field.
	 * Returns null if no result exists for the fieldName specified.
	 */
	BindingResult get(String fieldName);

}
