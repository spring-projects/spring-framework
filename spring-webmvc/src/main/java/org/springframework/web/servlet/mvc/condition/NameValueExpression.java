/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.condition;

import org.jspecify.annotations.Nullable;

import org.springframework.web.bind.annotation.RequestMapping;

/**
 * A contract for {@code "name!=value"} style expression used to specify request
 * parameters and request header conditions in {@code @RequestMapping}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @param <T> the value type
 * @see RequestMapping#params()
 * @see RequestMapping#headers()
 */
public interface NameValueExpression<T> {

	String getName();

	@Nullable T getValue();

	boolean isNegated();

}
