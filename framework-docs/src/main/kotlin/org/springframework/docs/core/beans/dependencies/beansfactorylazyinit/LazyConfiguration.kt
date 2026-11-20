/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.beans.dependencies.beansfactorylazyinit

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

// tag::snippet[]
@Configuration
@Lazy
class LazyConfiguration {
	// No bean will be pre-instantiated...
}
// end::snippet[]