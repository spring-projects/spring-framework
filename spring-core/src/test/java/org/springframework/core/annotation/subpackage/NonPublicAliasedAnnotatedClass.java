/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation.subpackage;

/**
 * Class annotated with a non-public (i.e., package private) custom annotation
 * that uses {@code @AliasFor}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@NonPublicAliasedAnnotation(name = "test", path = "/test")
class NonPublicAliasedAnnotatedClass {
}
