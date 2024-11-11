/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.servlet.setup;

import java.util.Locale;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * A {@link ViewResolver} that always returns same View.
 *
 * @author Rob Winch
 * @since 6.2
 */
class StaticViewResolver implements ViewResolver {

	private final View view;

	public StaticViewResolver(View view) {
		this.view = view;
	}

	@Override
	@Nullable
	public View resolveViewName(String viewName, Locale locale) {
		return this.view;
	}
}
