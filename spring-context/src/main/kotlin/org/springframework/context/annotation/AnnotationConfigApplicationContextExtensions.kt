/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation

/**
 * Extension for [AnnotationConfigApplicationContext] allowing
 * `AnnotationConfigApplicationContext { ... }` style initialization.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
@Deprecated("Use regular apply method instead.", replaceWith = ReplaceWith("AnnotationConfigApplicationContext().apply(configure)"))
fun AnnotationConfigApplicationContext(configure: AnnotationConfigApplicationContext.() -> Unit) =
		AnnotationConfigApplicationContext().apply(configure)
