/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind.support;

import org.springframework.web.bind.WebDataBinder;

/**
 * Callback interface for initializing a {@link WebDataBinder} for performing
 * data binding in the context of a specific web request.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 2.5
 */
public interface WebBindingInitializer {

	/**
	 * Initialize the given DataBinder.
	 * @param binder the DataBinder to initialize
	 * @since 5.0
	 */
	void initBinder(WebDataBinder binder);

}
