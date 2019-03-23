/*
 * Copyright 2002-2007 the original author or authors.
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
import org.springframework.web.context.request.WebRequest;

/**
 * Callback interface for initializing a {@link org.springframework.web.bind.WebDataBinder}
 * for performing data binding in the context of a specific web request.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public interface WebBindingInitializer {

	/**
	 * Initialize the given DataBinder for the given request.
	 * @param binder the DataBinder to initialize
	 * @param request the web request that the data binding happens within
	 */
	void initBinder(WebDataBinder binder, WebRequest request);

}
