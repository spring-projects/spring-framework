/*
 * Copyright 2002-2023 the original author or authors.
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

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
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
	@Nullable
	public String resolveName(MethodParameter parameter) {
		BindParam bindParam = parameter.getParameterAnnotation(BindParam.class);
		if (bindParam != null) {
			if (StringUtils.hasText(bindParam.value())) {
				return bindParam.value();
			}
		}
		return null;
	}

}
