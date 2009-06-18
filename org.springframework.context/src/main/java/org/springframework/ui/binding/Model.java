package org.springframework.ui.binding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Model {

	/**
	 * The name of the model
	 */
	String value() default "";

	/**
	 * Configures strict model binding.
	 * @see Binder#setStrict(boolean)
	 */
	boolean strict() default false;

}
