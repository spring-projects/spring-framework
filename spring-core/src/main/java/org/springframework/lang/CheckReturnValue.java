/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Specifies that the method return value must be used.
 *
 * <p>Inspired by {@code org.jetbrains.annotations.CheckReturnValue}, this variant
 * has been introduced in the {@code org.springframework.lang} package to avoid
 * requiring an extra dependency, while still following similar semantics.
 *
 * <p>This annotation should not be used if the return value of the method
 * provides only <i>additional</i> information. For example, the main purpose
 * of {@link java.util.Collection#add(Object)} is to modify the collection
 * and the return value is only interesting when adding an element to a set,
 * to see if the set already contained that element before.
 *
 * <p>When used on a type, the annotation applies to all constructors and all
 * methods that do not return {@code void}.
 *
 * @author Sebastien Deleuze
 * @since 6.2
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CheckReturnValue {
}
