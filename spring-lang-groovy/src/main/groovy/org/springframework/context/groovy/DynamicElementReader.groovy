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
package org.springframework.context.groovy

import groovy.xml.StreamingMarkupBuilder
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException
import org.springframework.beans.factory.parsing.Location
import org.springframework.beans.factory.parsing.Problem
import org.springframework.beans.factory.xml.DefaultDocumentLoader
import org.springframework.beans.factory.xml.DelegatingEntityResolver
import org.springframework.beans.factory.xml.NamespaceHandler
import org.springframework.beans.factory.xml.ParserContext
import org.springframework.core.io.ByteArrayResource
import org.springframework.util.xml.SimpleSaxErrorHandler
import org.springframework.util.xml.XmlValidationModeDetector
import org.w3c.dom.Element
import org.xml.sax.EntityResolver
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource

/**
 * Used by GroovyBeanDefinitionReader to read a Spring namespace expression in the Groovy DSL
 *
 * @see GroovyBeanDefinitionReader
 *
 */

class DynamicElementReader extends GroovyObjectSupport{


    private static final LOG = LogFactory.getLog(GroovyBeanDefinitionReader)

    private Map xmlNamespaces
    private String rootNamespace
    ErrorHandler errorHandler = new SimpleSaxErrorHandler(LOG);
    int validationMode = XmlValidationModeDetector.VALIDATION_NONE
    EntityResolver entityResolver = new DelegatingEntityResolver(DynamicElementReader.class.getClassLoader())
    ParserContext parserContext
    NamespaceHandler namespaceHandler
    BeanConfiguration beanConfiguration
    boolean beanDecorator  = false
    boolean firstCall = true

    public DynamicElementReader(String namespace, Map namespaceMap=Collections.EMPTY_MAP, NamespaceHandler namespaceHandler = null, ParserContext parserContext = null) {
        super();
        this.xmlNamespaces = namespaceMap;
        this.rootNamespace = namespace
        this.namespaceHandler = namespaceHandler
        this.parserContext = parserContext
    }

    void setClassLoader(ClassLoader classLoader) {
        entityResolver = new DelegatingEntityResolver(classLoader)
    }

    /**
     * Hook that subclass or anonymous classes can overwrite to implement custom behavior after invocation
     * completes
     */
    protected void afterInvocation() {
        // NOOP
    }
    
    @Override
    public Object invokeMethod(String name, Object args) {
        boolean invokeAfterInterceptor = false
        if(firstCall) {
            invokeAfterInterceptor = true
            firstCall=false
        }
        if(name.equals("doCall")) {
            def callable = args[0]
            callable.resolveStrategy = Closure.DELEGATE_FIRST
            callable.delegate = this
            def result = callable.call()
            if(invokeAfterInterceptor) {
                afterInvocation()
            }
            return result            
        }
        else {
            StreamingMarkupBuilder builder = new StreamingMarkupBuilder();
            def myNamespaces = xmlNamespaces
            def myNamespace = rootNamespace

            def callable = {
                for(namespace in myNamespaces) {
                    mkp.declareNamespace( [(namespace.key):namespace.value] )
                }
                if(args && (args[-1] instanceof Closure)) {
                    args[-1].resolveStrategy = Closure.DELEGATE_FIRST
                    args[-1].delegate = builder
                }
                delegate."$myNamespace"."$name"(*args)
            }

            callable.resolveStrategy=Closure.DELEGATE_FIRST
            callable.delegate = builder
            def writable = builder.bind( callable )

            def sw = new StringWriter()
            writable.writeTo(sw)

            def documentLoader = new DefaultDocumentLoader()
            InputSource is = new InputSource(new StringReader(sw.toString()))
            Element element = documentLoader.loadDocument(is, entityResolver, errorHandler, validationMode, true).getDocumentElement()

            parserContext?.delegate?.initDefaults element
            if(namespaceHandler && parserContext) {
                if(beanDecorator && beanConfiguration) {
                    BeanDefinitionHolder holder = new BeanDefinitionHolder(beanConfiguration.getBeanDefinition(), beanConfiguration.getName());
                    holder = namespaceHandler.decorate(element,holder, parserContext)
                    beanConfiguration.setBeanDefinition(holder.getBeanDefinition())
                }
                else {

                    def beanDefinition = namespaceHandler.parse(element, parserContext)
                    if(beanDefinition) {
                        beanConfiguration?.setBeanDefinition(beanDefinition) 
                    }
                }
            }
            else {
                throw new BeanDefinitionParsingException(new Problem("No namespace handler found for element ${sw}", new Location(parserContext?.readerContext?.resource ?: new ByteArrayResource(new byte[0]))))
            }
            if(invokeAfterInterceptor) {
                afterInvocation()
            }
            return element

        }
    }
}