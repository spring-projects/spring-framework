/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.hierarchies.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom context hierarchy configuration annotation that is itself meta-annotated
 * with {@link MetaContextHierarchyConfig @MetaContextHierarchyConfig}.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@MetaContextHierarchyConfig
public @interface MetaMetaContextHierarchyConfig {

}
