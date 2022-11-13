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

package org.springframework.aot.hint.annotation;

import java.lang.reflect.AnnotatedElement;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * A {@link ReflectiveProcessor} implementation that registers reflection hints
 * for data binding purpose (class, constructors, fields, properties, record
 * components, including types transitively used on properties and record components).
 *
 * @author Sebastien Deleuze
 * @since 6.0
 * @see RegisterReflectionForBinding @RegisterReflectionForBinding
 */
public class RegisterReflectionForBindingProcessor implements ReflectiveProcessor {

	private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();


	@Override
	public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
		RegisterReflectionForBinding registerReflection =
				AnnotationUtils.getAnnotation(element, RegisterReflectionForBinding.class);
		if (registerReflection != null) {
			Class<?>[] classes = registerReflection.classes();
			Assert.state(classes.length != 0, () -> "A least one class should be specified in " +
					"@RegisterReflectionForBinding attributes, and none was provided on " + element);
			for (Class<?> type : classes) {
				this.bindingRegistrar.registerReflectionHints(hints, type);
			}
		}
	}

}
