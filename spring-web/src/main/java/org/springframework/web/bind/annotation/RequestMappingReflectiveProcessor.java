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

package org.springframework.web.bind.annotation;

import java.lang.reflect.Method;

import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.aot.hint.annotation.SimpleReflectiveProcessor;

/**
 * {@link ReflectiveProcessor} implementation for {@link RequestMapping}
 * annotated types. On top of registering reflection hints for invoking
 * the annotated method, this implementation handles return types that
 * are serialized as well as TBD.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
class RequestMappingReflectiveProcessor extends SimpleReflectiveProcessor {

	@Override
	protected void registerMethodHint(ReflectionHints hints, Method method) {
		super.registerMethodHint(hints, method);
		// TODO
	}
}
