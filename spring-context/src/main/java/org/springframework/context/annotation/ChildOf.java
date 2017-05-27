package org.springframework.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to reference an xml-declared parent.
 * <p>
 * Behaves like bean-parent attribute. All properties are inherited.
 * <p>
 * To enable the corresponding ChildOfConfigurer, add it to your spring configuration. For Example like this:
 * <p>
 * <code>
 * &lt;bean class="org.springframework.context.annotation.ChildOfConfigurer"/&gt;
 * </code>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChildOf {
    String parent() default "";
}
