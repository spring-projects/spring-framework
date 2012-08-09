/*
 * Copyright 2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package org.springframework.context.groovy;


import org.springframework.context.ApplicationContext 
import org.springframework.core.io.ClassPathResource

class GroovyBeanDefinitionApplicationContext {
	
	@Delegate
	private ApplicationContext applicationContext
	
	GroovyBeanDefinitionApplicationContext(String resource) {
		this([resource] as String[])
	}
	
	GroovyBeanDefinitionApplicationContext(String[] resources) {
	    initializeApplicationContext resources
	}
	
	GroovyBeanDefinitionApplicationContext(String resource, Class clazz) {
	    this([resource] as String[], clazz)
    }
    
	GroovyBeanDefinitionApplicationContext(String[] resources, Class clazz) {
		def classPathResources = resources.collect { new ClassPathResource(it, clazz) }
		initializeApplicationContext classPathResources
	}
	
	private initializeApplicationContext(resources) {
	    def bb = new GroovyBeanDefinitionReader()
	    resources.each { bb.loadBeans it }
	    applicationContext = bb.createApplicationContext()
	}
	
	def propertyMissing(String propertyName) {
	    applicationContext.getBean(propertyName)
    }
}
