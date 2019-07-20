package org.springframework.test.context;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Container for repeatable annotation {@link TestPropertySource}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TestPropertySources {

	/**
	 * array of annotation values
	 * @return value
	 */
	TestPropertySource[] value();
}
