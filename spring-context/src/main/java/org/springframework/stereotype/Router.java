package org.springframework.stereotype;

import org.springframework.core.annotation.AliasFor;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Configuration
public @interface Router {

	/**
	 * Alias for {@link Configuration#value}.
	 */
	@AliasFor(annotation = Configuration.class)
	String value() default "";

}
