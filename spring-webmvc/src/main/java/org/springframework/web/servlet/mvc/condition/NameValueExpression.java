/*
 * Copyright 2002-present the original author or authors.
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
