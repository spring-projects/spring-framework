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
package org.springframework.ui.binding.binder;

import java.util.List;

/**
 * The results of a bind operation.
 * @author Keith Donald
 * @since 3.0
 * @see Binder#bind(UserValues)
 */
public interface BindingResults extends Iterable<BindingResult> {

	/**
	 * The subset of BindingResults that were successful.
	 */
	BindingResults successes();

	/**
	 * The subset of BindingResults that failed.
	 */
	BindingResults failures();

	/**
	 * The number of results.
	 */
	int size();

	/**
	 * The BindingResult at the specified index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds
	 */
	BindingResult get(int index);

	/**
	 * The ordered list of properties for which a {@link BindingResult} was returned.
	 */
	List<String> properties();
	
}
