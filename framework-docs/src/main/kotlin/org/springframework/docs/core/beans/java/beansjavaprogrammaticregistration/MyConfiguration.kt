/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.beans.java.beansjavaprogrammaticregistration

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

// tag::snippet[]
@Configuration
@Import(MyBeanRegistrar::class)
class MyConfiguration {
}
// end::snippet[]