/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aop.ataspectj.aopataspectjexample.service;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// tag::snippet[]
@Retention(RetentionPolicy.RUNTIME)
// marker annotation
public @interface Idempotent {
}
// end::snippet[]
