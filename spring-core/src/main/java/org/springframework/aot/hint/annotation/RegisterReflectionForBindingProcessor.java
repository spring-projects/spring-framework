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

package org.springframework.aot.hint.annotation;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;

/**
 * A {@link ReflectiveProcessor} implementation that registers reflection hints
 * for data binding purpose, that is class, constructors, fields, properties,
 * record components, including types transitively used on properties and record
 * components.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @since 6.0
 * @see RegisterReflectionForBinding @RegisterReflectionForBinding
 */
class RegisterReflectionForBindingProcessor extends RegisterReflectionReflectiveProcessor {

	private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

	@Override
	protected void registerReflectionHints(ReflectionHints hints, Class<?> target, MemberCategory[] memberCategories) {
		this.bindingRegistrar.registerReflectionHints(hints, target);
	}

}
