/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.core.annotation.AliasFor;

@Retention(RetentionPolicy.RUNTIME)
public @interface EnclosingAnnotation {

	@AliasFor("nested2")
	NestedAnnotation nested1() default @NestedAnnotation;

	@AliasFor("nested1")
	NestedAnnotation nested2() default @NestedAnnotation;

}
