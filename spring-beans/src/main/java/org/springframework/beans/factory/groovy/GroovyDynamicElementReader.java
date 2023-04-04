/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.factory.groovy;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.Writable;
import groovy.xml.StreamingMarkupBuilder;
import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.lang.Nullable;

/**
 * Used by GroovyBeanDefinitionReader to read a Spring XML namespace expression
 * in the Groovy DSL.
 *
 * @author Jeff Brown
 * @author Juergen Hoeller
 * @author Dave Syer
 * @since 4.0
 */
class GroovyDynamicElementReader extends GroovyObjectSupport {

	private final String rootNamespace;

	private final Map<String, String> xmlNamespaces;

	private final BeanDefinitionParserDelegate delegate;

	private final GroovyBeanDefinitionWrapper beanDefinition;

	protected final boolean decorating;

	private boolean callAfterInvocation = true;


	public GroovyDynamicElementReader(String namespace, Map<String, String> namespaceMap,
			BeanDefinitionParserDelegate delegate, GroovyBeanDefinitionWrapper beanDefinition, boolean decorating) {

		this.rootNamespace = namespace;
		this.xmlNamespaces = namespaceMap;
		this.delegate = delegate;
		this.beanDefinition = beanDefinition;
		this.decorating = decorating;
	}


	@Override
	@Nullable
	public Object invokeMethod(String name, Object obj) {
		Object[] args = (Object[]) obj;
		if (name.equals("doCall")) {
			@SuppressWarnings("unchecked")
			Closure<Object> callable = (Closure<Object>) args[0];
			callable.setResolveStrategy(Closure.DELEGATE_FIRST);
			callable.setDelegate(this);
			Object result = callable.call();

			if (this.callAfterInvocation) {
				afterInvocation();
				this.callAfterInvocation = false;
			}
			return result;
		}
		else {
			StreamingMarkupBuilder builder = new StreamingMarkupBuilder();
			String myNamespace = this.rootNamespace;
			Map<String, String> myNamespaces = this.xmlNamespaces;

			@SuppressWarnings("serial")
			Closure<Object> callable = new Closure<>(this) {
				@Override
				public Object call(Object... arguments) {
					((GroovyObject) getProperty("mkp")).invokeMethod("declareNamespace", new Object[] {myNamespaces});
					int len = args.length;
					if (len > 0 && args[len-1] instanceof Closure<?> callable) {
						callable.setResolveStrategy(Closure.DELEGATE_FIRST);
						callable.setDelegate(builder);
					}
					return ((GroovyObject) ((GroovyObject) getDelegate()).getProperty(myNamespace)).invokeMethod(name, args);
				}
			};

			callable.setResolveStrategy(Closure.DELEGATE_FIRST);
			callable.setDelegate(builder);
			Writable writable = (Writable) builder.bind(callable);
			StringWriter sw = new StringWriter();
			try {
				writable.writeTo(sw);
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}

			Element element = this.delegate.getReaderContext().readDocumentFromString(sw.toString()).getDocumentElement();
			this.delegate.initDefaults(element);
			if (this.decorating) {
				BeanDefinitionHolder holder = this.beanDefinition.getBeanDefinitionHolder();
				holder = this.delegate.decorateIfRequired(element, holder, null);
				this.beanDefinition.setBeanDefinitionHolder(holder);
			}
			else {
				BeanDefinition beanDefinition = this.delegate.parseCustomElement(element);
				if (beanDefinition != null) {
					this.beanDefinition.setBeanDefinition((AbstractBeanDefinition) beanDefinition);
				}
			}
			if (this.callAfterInvocation) {
				afterInvocation();
				this.callAfterInvocation = false;
			}
			return element;
		}
	}

	/**
	 * Hook that subclasses or anonymous classes can override to implement custom behavior
	 * after invocation completes.
	 */
	protected void afterInvocation() {
		// NOOP
	}

}
