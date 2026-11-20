/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aop.ataspectj.aopataspectjexample.service

// tag::snippet[]
@Retention(AnnotationRetention.RUNTIME)
// marker annotation
annotation class Idempotent
// end::snippet[]