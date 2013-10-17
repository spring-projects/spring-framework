/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans.factory.groovy

import groovy.xml.StreamingMarkupBuilder
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate
import org.w3c.dom.Element

/**
 * Used by GroovyBeanDefinitionReader to read a Spring XML namespace expression
 * in the Groovy DSL.
 *
 * @author Jeff Brown
 * @author Juergen Hoeller
 * @since 4.0
 */
@groovy.transform.PackageScope
class GroovyDynamicElementReader extends GroovyObjectSupport {

	private final String rootNamespace

	private final Map<String, String> xmlNamespaces

	private final BeanDefinitionParserDelegate delegate

	private final GroovyBeanDefinitionWrapper beanDefinition

	protected final boolean decorating;

	private boolean callAfterInvocation = true


    public GroovyDynamicElementReader(String namespace, Map<String, String> namespaceMap,
			  BeanDefinitionParserDelegate delegate, GroovyBeanDefinitionWrapper beanDefinition, boolean decorating) {
        super();
		this.rootNamespace = namespace
        this.xmlNamespaces = namespaceMap
        this.delegate = delegate
		this.beanDefinition = beanDefinition;
		this.decorating = decorating;
    }


    @Override
    public Object invokeMethod(String name, Object args) {
        if (name.equals("doCall")) {
            def callable = args[0]
            callable.resolveStrategy = Closure.DELEGATE_FIRST
            callable.delegate = this
            def result = callable.call()

			if (this.callAfterInvocation) {
				afterInvocation()
				this.callAfterInvocation = false
			}
            return result
        }

        else {
            StreamingMarkupBuilder builder = new StreamingMarkupBuilder();
			def myNamespace = this.rootNamespace
            def myNamespaces = this.xmlNamespaces

            def callable = {
                for (namespace in myNamespaces) {
                    mkp.declareNamespace([(namespace.key):namespace.value])
                }
                if (args && (args[-1] instanceof Closure)) {
                    args[-1].resolveStrategy = Closure.DELEGATE_FIRST
                    args[-1].delegate = builder
                }
                delegate."$myNamespace"."$name"(*args)
            }

            callable.resolveStrategy = Closure.DELEGATE_FIRST
            callable.delegate = builder
            def writable = builder.bind(callable)
            def sw = new StringWriter()
            writable.writeTo(sw)

			Element element = this.delegate.readerContext.readDocumentFromString(sw.toString()).documentElement
			this.delegate.initDefaults(element)
			if (this.decorating) {
				BeanDefinitionHolder holder = this.beanDefinition.beanDefinitionHolder;
				holder = this.delegate.decorateIfRequired(element, holder, null)
				this.beanDefinition.setBeanDefinitionHolder(holder)
			}
			else {
				def beanDefinition = this.delegate.parseCustomElement(element)
				if (beanDefinition) {
					this.beanDefinition.setBeanDefinition(beanDefinition)
				}
			}
            if (this.callAfterInvocation) {
                afterInvocation()
				this.callAfterInvocation = false
            }
            return element
        }
    }

	/**
	 * Hook that subclass or anonymous classes can overwrite to implement custom behavior
	 * after invocation completes.
	 */
	protected void afterInvocation() {
		// NOOP
	}

}
