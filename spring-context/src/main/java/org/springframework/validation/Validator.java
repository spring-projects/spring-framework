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

/**
 * A validator for application-specific objects.
 *
 * <p>This interface is totally divorced from any infrastructure
 * or context; that is to say it is not coupled to validating
 * only objects in the web tier, the data-access tier, or the
 * whatever-tier. As such it is amenable to being used in any layer
 * of an application, and supports the encapsulation of validation
 * logic as a first-class citizen in its own right.
 *
 * <p>Implementations can be created via the static factory methods
 * {@link #forInstanceOf(Class, BiConsumer)} or
 * {@link #forType(Class, BiConsumer)}.
 * Below is a simple but complete {@code Validator} that validates that the
 * various {@link String} properties of a {@code UserLogin} instance are not
 * empty (they are not {@code null} and do not consist
 * wholly of whitespace), and that any password that is present is
 * at least {@code 'MINIMUM_PASSWORD_LENGTH'} characters in length.
 *
 * <pre class="code">Validator userLoginValidator = Validator.forInstance(UserLogin.class, (login, errors) -> {
 *   ValidationUtils.rejectIfEmptyOrWhitespace(errors, "userName", "field.required");
 *   ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "field.required");
 *   if (login.getPassword() != null
 *         &amp;&amp; login.getPassword().trim().length() &lt; MINIMUM_PASSWORD_LENGTH) {
 *      errors.rejectValue("password", "field.min.length",
 *            new Object[]{Integer.valueOf(MINIMUM_PASSWORD_LENGTH)},
 *            "The password must be at least [" + MINIMUM_PASSWORD_LENGTH + "] characters in length.");
 *   }
 * });</pre>
 *
 * <p>See also the Spring reference manual for a fuller discussion of the
 * {@code Validator} interface and its role in an enterprise application.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Toshiaki Maki
 * @author Arjen Poutsma
 * @see SmartValidator
 * @see Errors
 * @see ValidationUtils
 * @see DataBinder#setValidator
 */
public interface Validator {

	/**
	 * Can this {@link Validator} {@link #validate(Object, Errors) validate}
	 * instances of the supplied {@code clazz}?
	 * <p>This method is <i>typically</i> implemented like so:
	 * <pre class="code">return Foo.class.isAssignableFrom(clazz);</pre>
	 * (Where {@code Foo} is the class (or superclass) of the actual
	 * object instance that is to be {@link #validate(Object, Errors) validated}.)
	 * @param clazz the {@link Class} that this {@link Validator} is
	 * being asked if it can {@link #validate(Object, Errors) validate}
	 * @return {@code true} if this {@link Validator} can indeed
	 * {@link #validate(Object, Errors) validate} instances of the
	 * supplied {@code clazz}
	 */
	boolean supports(Class<?> clazz);

	/**
	 * Validate the given {@code target} object which must be of a
	 * {@link Class} for which the {@link #supports(Class)} method
	 * typically has returned (or would return) {@code true}.
	 * <p>The supplied {@link Errors errors} instance can be used to report
	 * any resulting validation errors, typically as part of a larger
	 * binding process which this validator is meant to participate in.
	 * Binding errors have typically been pre-registered with the
	 * {@link Errors errors} instance before this invocation already.
	 * @param target the object that is to be validated
	 * @param errors contextual state about the validation process
	 * @see ValidationUtils
	 */
	void validate(Object target, Errors errors);

	/**
	 * Validate the given {@code target} object individually.
	 * <p>Delegates to the common {@link #validate(Object, Errors)} method.
	 * The returned {@link Errors errors} instance can be used to report
	 * any resulting validation errors for the specific target object, e.g.
	 * {@code if (validator.validateObject(target).hasErrors()) ...} or
	 * {@code validator.validateObject(target).failOnError(IllegalStateException::new));}.
	 * <p>Note: This validation call comes with limitations in the {@link Errors}
	 * implementation used, in particular no support for nested paths.
	 * If this is insufficient for your purposes, call the regular
	 * {@link #validate(Object, Errors)} method with a binding-capable
	 * {@link Errors} implementation such as {@link BeanPropertyBindingResult}.
	 * @param target the object that is to be validated
	 * @return resulting errors from the validation of the given object
	 * @since 6.1
	 * @see SimpleErrors
	 */
	default Errors validateObject(Object target) {
		Errors errors = new SimpleErrors(target);
		validate(target, errors);
		return errors;
	}


	/**
	 * Return a {@code Validator} that checks whether the target object
	 * {@linkplain Class#isAssignableFrom(Class) is an instance of}
	 * {@code targetClass}, applying the given {@code delegate} to populate
	 * {@link Errors} if it is.
	 * <p>For instance:
	 * <pre class="code">Validator passwordEqualsValidator = Validator.forInstanceOf(PasswordResetForm.class, (form, errors) -> {
	 *   if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
	 * 	   errors.rejectValue("confirmPassword",
	 * 	         "PasswordEqualsValidator.passwordResetForm.password",
	 * 	         "password and confirm password must be same.");
	 * 	   }
	 * 	 });</pre>
	 * @param targetClass the class supported by the returned validator
	 * @param delegate function invoked with the target object, if it is an
	 * instance of type T
	 * @param <T> the target object type
	 * @return the created {@code Validator}
	 * @since 6.1
	 */
	static <T> Validator forInstanceOf(Class<T> targetClass, BiConsumer<T, Errors> delegate) {
		return new TypedValidator<>(targetClass, targetClass::isAssignableFrom, delegate);
	}

	/**
	 * Return a {@code Validator} that checks whether the target object's class
	 * is identical to {@code targetClass}, applying the given {@code delegate}
	 * to populate {@link Errors} if it is.
	 * <p>For instance:
	 * <pre class="code">Validator passwordEqualsValidator = Validator.forType(PasswordResetForm.class, (form, errors) -> {
	 *   if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
	 * 	   errors.rejectValue("confirmPassword",
	 * 	         "PasswordEqualsValidator.passwordResetForm.password",
	 * 	         "password and confirm password must be same.");
	 * 	   }
	 * 	 });</pre>
	 * @param targetClass the exact class supported by the returned validator (no subclasses)
	 * @param delegate function invoked with the target object, if it is an
	 * instance of type T
	 * @param <T> the target object type
	 * @return the created {@code Validator}
	 * @since 6.1
	 */
	static <T> Validator forType(Class<T> targetClass, BiConsumer<T, Errors> delegate) {
		return new TypedValidator<>(targetClass, targetClass::equals, delegate);
	}

}
