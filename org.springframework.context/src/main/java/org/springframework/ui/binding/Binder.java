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

import java.util.Map;

/**
 * Binds user-entered values to properties of a model object.
 * @author Keith Donald
 * @since 3.0
 * @see #bind(Map)
 */
public interface Binder extends BindingFactory {

	/**
	 * Bind the source values to the properties of the model.
	 * A result is returned for each registered {@link Binding}.
	 * @param sourceValues the source values to bind
	 * @return the results of the binding operation
	 * @throws MissingSourceValuesException when the sourceValues Map is missing entries for required bindings
	 */
	BindingResults bind(Map<String, ? extends Object> sourceValues);

}