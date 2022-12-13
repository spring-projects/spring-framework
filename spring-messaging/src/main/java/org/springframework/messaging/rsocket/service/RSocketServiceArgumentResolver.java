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

package org.springframework.messaging.rsocket.service;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;

/**
 * Resolve an argument from an {@link RSocketExchange @RSocketExchange}-annotated
 * method to one or more RSocket request values.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public interface RSocketServiceArgumentResolver {

	/**
	 * Resolve the argument value.
	 * @param argument the argument value
	 * @param parameter the method parameter for the argument
	 * @param requestValues builder to add RSocket request values to
	 * @return {@code true} if the argument was resolved, {@code false} otherwise
	 */
	boolean resolve(@Nullable Object argument, MethodParameter parameter, RSocketRequestValues.Builder requestValues);

}
