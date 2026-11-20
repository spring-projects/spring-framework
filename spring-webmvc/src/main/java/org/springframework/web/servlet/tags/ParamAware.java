/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags;

/**
 * Allows implementing tag to utilize nested {@code spring:param} tags.
 *
 * @author Scott Andrews
 * @since 3.0
 * @see ParamTag
 */
public interface ParamAware {

	/**
	 * Callback hook for nested spring:param tags to pass their value
	 * to the parent tag.
	 * @param param the result of the nested {@code spring:param} tag
	 */
	void addParam(Param param);

}
