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

package org.springframework.http.converter.json;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers binding reflection hints
 * for {@link ProblemDetail} serialization support with Jackson.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 6.0.5
 */
class ProblemDetailRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();
		bindingRegistrar.registerReflectionHints(hints.reflection(), ProblemDetail.class);
		if (ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader)) {
			bindingRegistrar.registerReflectionHints(hints.reflection(), ProblemDetailJacksonXmlMixin.class);
		}
		else if (ClassUtils.isPresent("com.fasterxml.jackson.annotation.JacksonAnnotation", classLoader)) {
			bindingRegistrar.registerReflectionHints(hints.reflection(), ProblemDetailJacksonMixin.class);
		}
	}

}
