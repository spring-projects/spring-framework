/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.validation;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.springframework.util.Assert;

/**
 * Validator instance returned by {@link Validator#forInstanceOf(Class, BiConsumer)}
 * and {@link Validator#forType(Class, BiConsumer)}.
 *
 * @author Toshiaki Maki
 * @author Arjen Poutsma
 * @since 6.1
 * @param <T> the target object type
 */
final class TypedValidator<T> implements Validator {

	private final Class<T> targetClass;

	private final Predicate<Class<?>> supports;

	private final BiConsumer<T, Errors> validate;


	public TypedValidator(Class<T> targetClass, Predicate<Class<?>> supports, BiConsumer<T, Errors> validate) {
		Assert.notNull(targetClass, "TargetClass must not be null");
		Assert.notNull(supports, "Supports function must not be null");
		Assert.notNull(validate, "Validate function must not be null");

		this.targetClass = targetClass;
		this.supports = supports;
		this.validate = validate;
	}


	@Override
	public boolean supports(Class<?> clazz) {
		return this.supports.test(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		this.validate.accept(this.targetClass.cast(target), errors);
	}

}
