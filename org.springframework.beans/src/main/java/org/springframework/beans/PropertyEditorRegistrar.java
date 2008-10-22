/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.beans;

/**
 * Interface for strategies that register custom
 * {@link java.beans.PropertyEditor property editors} with a
 * {@link org.springframework.beans.PropertyEditorRegistry property editor registry}.
 *
 * <p>This is particularly useful when you need to use the same set of
 * property editors in several different situations: write a corresponding
 * registrar and reuse that in each case.
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see PropertyEditorRegistry
 * @see java.beans.PropertyEditor
 */
public interface PropertyEditorRegistrar {
	
	/**
	 * Register custom {@link java.beans.PropertyEditor PropertyEditors} with
	 * the given <code>PropertyEditorRegistry</code>.
	 * <p>The passed-in registry will usually be a {@link BeanWrapper} or a
	 * {@link org.springframework.validation.DataBinder DataBinder}.
	 * <p>It is expected that implementations will create brand new
	 * <code>PropertyEditors</code> instances for each invocation of this
	 * method (since <code>PropertyEditors</code> are not threadsafe).
	 * @param registry the <code>PropertyEditorRegistry</code> to register the
	 * custom <code>PropertyEditors</code> with
	 */
	void registerCustomEditors(PropertyEditorRegistry registry);

}
