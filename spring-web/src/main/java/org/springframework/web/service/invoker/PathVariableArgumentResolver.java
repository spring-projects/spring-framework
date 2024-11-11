/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.service.invoker;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * {@link HttpServiceArgumentResolver} for {@link PathVariable @PathVariable}
 * annotated arguments.
 *
 * <p>The argument may be a single variable value or a {@code Map} with multiple
 * variables and values. Each value may be a String or an Object to be converted
 * to a String through the configured {@link ConversionService}.
 *
 * <p>If the value is required but {@code null}, {@link IllegalArgumentException}
 * is raised. The value is not required if:
 * <ul>
 * <li>{@link PathVariable#required()} is set to {@code false}
 * <li>The argument is declared as {@link java.util.Optional}
 * </ul>
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public class PathVariableArgumentResolver extends AbstractNamedValueArgumentResolver {


	public PathVariableArgumentResolver(ConversionService conversionService) {
		super(conversionService);
	}


	@Override
	@Nullable
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		PathVariable annot = parameter.getParameterAnnotation(PathVariable.class);
		return (annot == null ? null :
				new NamedValueInfo(annot.name(), annot.required(), null, "path variable", false));
	}

	@Override
	protected void addRequestValue(
			String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		requestValues.setUriVariable(name, (String) value);
	}

}
