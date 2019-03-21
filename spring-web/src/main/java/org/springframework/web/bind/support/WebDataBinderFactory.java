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

package org.springframework.web.bind.support;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * A factory for creating a {@link WebDataBinder} instance for a named target object.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public interface WebDataBinderFactory {

	/**
	 * Create a {@link WebDataBinder} for the given object.
	 * @param webRequest the current request
	 * @param target the object to create a data binder for, or {@code null} if creating a binder for a simple type
	 * @param objectName the name of the target object
	 * @return the created {@link WebDataBinder} instance, never null
	 * @throws Exception raised if the creation and initialization of the data binder fails
	 */
	WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName) throws Exception;

}
