/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
