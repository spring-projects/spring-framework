/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.gh24375;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnclosingAnnotation {

	@AliasFor("nested2")
	NestedAnnotation nested1() default @NestedAnnotation;

	@AliasFor("nested1")
	NestedAnnotation nested2() default @NestedAnnotation;

}
