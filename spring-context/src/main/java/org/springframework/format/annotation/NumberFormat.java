/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a field or method parameter should be formatted as a number.
 *
 * <p>Supports formatting by style or custom pattern string. Can be applied to
 * any JDK {@code Number} types such as {@code Double} and {@code Long}.
 *
 * <p>For style-based formatting, set the {@link #style} attribute to the desired
 * {@link Style}. For custom formatting, set the {@link #pattern} attribute to the
 * desired number pattern, such as {@code "#,###.##"}.
 *
 * <p>Each attribute is mutually exclusive, so only set one attribute per
 * annotation (the one most convenient for your formatting needs). When the
 * {@link #pattern} attribute is specified, it takes precedence over the
 * {@link #style} attribute. When no annotation attributes are specified, the
 * default format applied is style-based for either number or currency,
 * depending on the annotated field or method parameter type.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.text.NumberFormat
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface NumberFormat {

	/**
	 * The style pattern to use to format the field or method parameter.
	 * <p>Defaults to {@link Style#DEFAULT} for general-purpose number formatting
	 * for most annotated types, except for money types which default to currency
	 * formatting.
	 * <p>Set this attribute when you wish to format your field or method parameter
	 * in accordance with a common style other than the default style.
	 */
	Style style() default Style.DEFAULT;

	/**
	 * The custom pattern to use to format the field or method parameter.
	 * <p>Defaults to an empty String, indicating no custom pattern has been
	 * specified.
	 * <p>Set this attribute when you wish to format your field or method parameter
	 * in accordance with a custom number pattern not represented by a style.
	 */
	String pattern() default "";


	/**
	 * Common number format styles.
	 */
	enum Style {

		/**
		 * The default format for the annotated type: typically 'number' but possibly
		 * 'currency' for a money type (for example, {@code javax.money.MonetaryAmount}).
		 * @since 4.2
		 */
		DEFAULT,

		/**
		 * The general-purpose number format for the current locale.
		 */
		NUMBER,

		/**
		 * The percent format for the current locale.
		 */
		PERCENT,

		/**
		 * The currency format for the current locale.
		 */
		CURRENCY
	}

}
