/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.beans.java.beansjavaprogrammaticregistration

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.web.servlet.function.router

// tag::snippet[]
class MyBeanRegistrar : BeanRegistrarDsl({
	registerBean<Foo>()
	registerBean(
		name = "bar",
		prototype = true,
		lazyInit = true,
		description = "Custom description") {
		Bar(bean<Foo>()) // Also possible with Bar(bean())
	}
	profile("baz") {
		registerBean { Baz("Hello World!") }
	}
	registerBean<MyRepository>()
	registerBean {
		myRouter(bean<MyRepository>()) // Also possible with myRouter(bean())
	}
})

fun myRouter(myRepository: MyRepository) = router {
	// ...
}
// end::snippet[]