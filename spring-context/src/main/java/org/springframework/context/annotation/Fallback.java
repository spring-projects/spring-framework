/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a bean qualifies as a fallback autowire candidate.
 * This is a companion and alternative to the {@link Primary} annotation.
 *
 * <p>If all beans but one among multiple matching candidates are marked
 * as a fallback, the remaining bean will be selected.
 *
 * <p>Just like primary beans, fallback beans only have an effect when
 * finding multiple candidates for single injection points.
 * All type-matching beans are included when autowiring arrays,
 * collections, maps, or ObjectProvider streams.
 *
 * @author Juergen Hoeller
 * @since 6.2
 * @see Primary
 * @see Lazy
 * @see Bean
 * @see org.springframework.beans.factory.config.BeanDefinition#setFallback
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Fallback {

}
