/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigadvancedxml

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component

// tag::snippet[]
@Component
class MyPostProcessor : BeanPostProcessor {

	override fun postProcessBeforeInitialization(bean: Any, name: String): Any {
		// ...
		return bean
	}
}
// end::snippet[]
