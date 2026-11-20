/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.json;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.http.ProblemDetail;
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
