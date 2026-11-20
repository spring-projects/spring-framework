/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags;

import jakarta.servlet.jsp.JspTagException;
import org.jspecify.annotations.Nullable;

/**
 * Allows implementing tag to utilize nested {@code spring:argument} tags.
 *
 * @author Nicholas Williams
 * @since 4.0
 * @see ArgumentTag
 */
public interface ArgumentAware {

	/**
	 * Callback hook for nested spring:argument tags to pass their value
	 * to the parent tag.
	 * @param argument the result of the nested {@code spring:argument} tag
	 */
	void addArgument(@Nullable Object argument) throws JspTagException;

}
