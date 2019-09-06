/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.view.freemarker;

import freemarker.template.Configuration;

/**
 * Interface to be implemented by objects that configure and manage a
 * FreeMarker Configuration object in a web environment. Detected and
 * used by {@link FreeMarkerView}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface FreeMarkerConfig {

	/**
	 * Return the FreeMarker Configuration object for the current
	 * web application context.
	 * <p>A FreeMarker Configuration object may be used to set FreeMarker
	 * properties and shared objects, and allows to retrieve templates.
	 * @return the FreeMarker Configuration
	 */
	Configuration getConfiguration();

}
