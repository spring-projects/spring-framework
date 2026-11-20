/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet;

/**
 * Builds a {@link MockMvc} instance.
 *
 * <p>See static factory methods in
 * {@link org.springframework.test.web.servlet.setup.MockMvcBuilders MockMvcBuilders}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface MockMvcBuilder {

	/**
	 * Build a {@link MockMvc} instance.
	 */
	MockMvc build();

}
