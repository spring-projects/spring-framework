/*
 * Copyright 2002-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvcservletcontexthierarchy

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer

// tag::snippet[]
class MyWebAppInitializer : AbstractAnnotationConfigDispatcherServletInitializer() {

	override fun getRootConfigClasses(): Array<Class<*>> {
		return arrayOf(RootConfig::class.java)
	}

	override fun getServletConfigClasses(): Array<Class<*>> {
		return arrayOf(App1Config::class.java)
	}

	override fun getServletMappings(): Array<String> {
		return arrayOf("/app1/*")
	}
}
// end::snippet[]

class RootConfig
class App1Config
