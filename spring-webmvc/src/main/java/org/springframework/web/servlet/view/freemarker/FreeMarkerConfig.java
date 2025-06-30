/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.view.freemarker;

import freemarker.ext.jakarta.jsp.TaglibFactory;
import freemarker.template.Configuration;

/**
 * Interface to be implemented by objects that configure and manage a
 * FreeMarker {@link Configuration} object in a web environment.
 *
 * <p>Detected and used by {@link FreeMarkerView}.
 *
 * <p>Note: Spring's FreeMarker support requires FreeMarker 2.3.33 or higher.
 *
 * @author Darren Davison
 * @author Rob Harrop
 * @since 03.03.2004
 * @see FreeMarkerConfigurer
 * @see FreeMarkerView
 */
public interface FreeMarkerConfig {

	/**
	 * Return the FreeMarker {@link Configuration} object for the current
	 * web application context.
	 * <p>A FreeMarker {@code Configuration} object may be used to set FreeMarker
	 * properties and shared objects, and allows to retrieve templates.
	 * @return the FreeMarker {@code Configuration}
	 */
	Configuration getConfiguration();

	/**
	 * Return the {@link TaglibFactory} used to enable JSP tags to be
	 * accessed from FreeMarker templates.
	 */
	TaglibFactory getTaglibFactory();

}
