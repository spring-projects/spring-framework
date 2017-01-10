package org.springframework.context.annotation

/**
 * Extension for [AnnotationConfigApplicationContext] providing
 * `AnnotationConfigApplicationContext { }` style initialization.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
object AnnotationConfigApplicationContextExtension {

    fun AnnotationConfigApplicationContext(configure: AnnotationConfigApplicationContext.()->Unit) =
            AnnotationConfigApplicationContext().apply(configure)
}
