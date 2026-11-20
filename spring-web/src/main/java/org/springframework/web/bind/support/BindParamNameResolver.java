/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind.support;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.annotation.BindParam;

/**
 * {@link org.springframework.validation.DataBinder.NameResolver} that determines
 * the bind value name from a {@link BindParam @BindParam} method parameter
 * annotation.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public final class BindParamNameResolver implements DataBinder.NameResolver {

	@Override
	public @Nullable String resolveName(MethodParameter parameter) {
		BindParam bindParam = parameter.getParameterAnnotation(BindParam.class);
		if (bindParam != null) {
			if (StringUtils.hasText(bindParam.value())) {
				return bindParam.value();
			}
		}
		return null;
	}

}
