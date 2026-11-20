/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aop.aopajltwspring

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableLoadTimeWeaving
import org.springframework.context.annotation.LoadTimeWeavingConfigurer
import org.springframework.instrument.classloading.LoadTimeWeaver
import org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver

// tag::snippet[]
@Configuration
@EnableLoadTimeWeaving
class CustomWeaverConfiguration : LoadTimeWeavingConfigurer {

	override fun getLoadTimeWeaver(): LoadTimeWeaver {
		return ReflectiveLoadTimeWeaver()
	}
}
// end::snippet[]