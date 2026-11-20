/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.view.freemarker;

import freemarker.template.Configuration;

/**
 * Interface to be implemented by objects that configure and manage a
 * FreeMarker {@link Configuration} object in a web environment.
 *
 * <p>Detected and used by {@link FreeMarkerView}.
 *
 * <p>Note: Spring's FreeMarker support requires FreeMarker 2.3.33 or higher.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
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

}
