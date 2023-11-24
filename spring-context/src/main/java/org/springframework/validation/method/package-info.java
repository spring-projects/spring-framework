/**
 * Abstractions and support classes for method validation, independent of the
 * underlying validation library.
 *
 * <p>The main abstractions:
 * <ul>
 * <li>{@link org.springframework.validation.method.MethodValidator} to apply
 * method validation, and return or handle the results.
 * <li>{@link org.springframework.validation.method.MethodValidationResult} and
 * related types to represent the results.
 * <li>{@link org.springframework.validation.method.MethodValidationException}
 * to expose method validation results.
 * </ul>
 */

@NonNullApi
@NonNullFields
package org.springframework.validation.method;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
