/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.remoting.jaxrpc.support;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.rpc.Service;
import javax.xml.rpc.encoding.TypeMapping;
import javax.xml.rpc.encoding.TypeMappingRegistry;

import org.apache.axis.encoding.ser.BeanDeserializerFactory;
import org.apache.axis.encoding.ser.BeanSerializerFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.remoting.jaxrpc.JaxRpcServicePostProcessor;
import org.springframework.util.ClassUtils;

/**
 * Axis-specific {@link JaxRpcServicePostProcessor} that registers bean
 * mappings for domain objects that follow the JavaBean pattern.
 *
 * <p>The same mappings are usually also registered at the server in
 * Axis' "server-config.wsdd" file.
 *
 * <p>To be registered as a service post-processor on a
 * {@link org.springframework.remoting.jaxrpc.LocalJaxRpcServiceFactoryBean} or
 * {@link org.springframework.remoting.jaxrpc.JaxRpcPortProxyFactoryBean},
 * carrying appropriate configuration.
 *
 * <p>Note: Without such explicit bean mappings, a complex type like a
 * domain object cannot be transferred via SOAP.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.apache.axis.encoding.ser.BeanSerializerFactory
 * @see org.apache.axis.encoding.ser.BeanDeserializerFactory
 * @see org.springframework.remoting.jaxrpc.LocalJaxRpcServiceFactoryBean#setServicePostProcessors
 * @see org.springframework.remoting.jaxrpc.JaxRpcPortProxyFactoryBean#setServicePostProcessors
 */
public class AxisBeanMappingServicePostProcessor implements JaxRpcServicePostProcessor, BeanClassLoaderAware {

	private String encodingStyleUri;

	private String typeNamespaceUri;

	private Map beanMappings;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	/**
	 * Set the encoding style URI to use for the type mapping.
	 * <p>A typical value is "http://schemas.xmlsoap.org/soap/encoding/",
	 * as suggested by the JAX-RPC javadoc. However, note that the default
	 * behavior of this post-processor is to register the type mapping
	 * as JAX-RPC default if no explicit encoding style URI is given.
	 * @see javax.xml.rpc.encoding.TypeMappingRegistry#register
	 * @see javax.xml.rpc.encoding.TypeMappingRegistry#registerDefault
	 */
	public void setEncodingStyleUri(String encodingStyleUri) {
		this.encodingStyleUri = encodingStyleUri;
	}

	/**
	 * Set the application-specific namespace to use for XML types,
	 * for example "urn:JPetStore".
	 * @see javax.xml.rpc.encoding.TypeMapping#register
	 */
	public void setTypeNamespaceUri(String typeNamespaceUri) {
		this.typeNamespaceUri = typeNamespaceUri;
	}

	/**
	 * Specify the bean mappings to register as String-String pairs,
	 * with the Java type name as key and the WSDL type name as value.
	 */
	public void setBeanMappings(Properties beanMappingProps) {
		if (beanMappingProps != null) {
			this.beanMappings = new HashMap(beanMappingProps.size());
			Enumeration propertyNames = beanMappingProps.propertyNames();
			while (propertyNames.hasMoreElements()) {
				String javaTypeName = (String) propertyNames.nextElement();
				String wsdlTypeName = beanMappingProps.getProperty(javaTypeName);
				this.beanMappings.put(javaTypeName, wsdlTypeName);
			}
		}
		else {
			this.beanMappings = null;
		}
	}

	/**
	 * Specify the bean mappings to register as Java types,
	 * with the WSDL type names inferred from the Java type names
	 * (using the short, that is, non-fully-qualified class name).
	 */
	public void setBeanClasses(Class[] beanClasses) {
		if (beanClasses != null) {
			this.beanMappings = new HashMap(beanClasses.length);
			for (int i = 0; i < beanClasses.length; i++) {
				Class beanClass = beanClasses[i];
				String wsdlTypeName = ClassUtils.getShortName(beanClass);
				this.beanMappings.put(beanClass, wsdlTypeName);
			}
		}
		else {
			this.beanMappings = null;
		}
	}

	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * Register the specified bean mappings on the given Service's
	 * {@link TypeMappingRegistry}.
	 * @see javax.xml.rpc.Service#getTypeMappingRegistry()
	 * @see #setBeanMappings
	 * @see #registerBeanMappings(javax.xml.rpc.encoding.TypeMapping)
	 */
	public void postProcessJaxRpcService(Service service) {
		TypeMappingRegistry registry = service.getTypeMappingRegistry();
		TypeMapping mapping = registry.createTypeMapping();

		registerBeanMappings(mapping);

		if (this.encodingStyleUri != null) {
			registry.register(this.encodingStyleUri, mapping);
		}
		else {
			registry.registerDefault(mapping);
		}
	}

	/**
	 * Perform the actual bean mapping registration.
	 * @param mapping the JAX-RPC {@link TypeMapping} to operate on
	 * @see #setBeanMappings
	 * @see #registerBeanMapping(javax.xml.rpc.encoding.TypeMapping, Class, String)
	 */
	protected void registerBeanMappings(TypeMapping mapping) {
		if (this.beanMappings != null) {
			for (Iterator it = this.beanMappings.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				Object key = entry.getKey();
				Class javaType = null;
				if (key instanceof Class) {
					javaType = (Class) key;
				}
				else {
					javaType = ClassUtils.resolveClassName((String) key, this.beanClassLoader);
				}
				String wsdlTypeName = (String) entry.getValue();
				registerBeanMapping(mapping, javaType, wsdlTypeName);
			}
		}
	}

	/**
	 * Register a bean mapping for the given Java type and WSDL type name.
	 * @param mapping the JAX-RPC {@link TypeMapping} to operate on
	 * @param javaType the Java type
	 * @param wsdlTypeName the WSDL type name (as a {@link String})
	 */
	protected void registerBeanMapping(TypeMapping mapping, Class javaType, String wsdlTypeName) {
		registerBeanMapping(mapping, javaType, getTypeQName(wsdlTypeName));
	}

	/**
	 * Register a bean mapping for the given Java type and WSDL type.
	 * @param mapping the JAX-RPC {@link TypeMapping} to operate on
	 * @param javaType the Java type
	 * @param wsdlType the WSDL type (as XML {@link QName})
	 */
	protected void registerBeanMapping(TypeMapping mapping, Class javaType, QName wsdlType) {
		mapping.register(javaType, wsdlType,
		    new BeanSerializerFactory(javaType, wsdlType),
		    new BeanDeserializerFactory(javaType, wsdlType));
	}

	/**
	 * Return a {@link QName} for the given name, relative to the
	 * {@link #setTypeNamespaceUri namespace URI} of this post-processor, if given.
	 */
	protected final QName getTypeQName(String name) {
		return (this.typeNamespaceUri != null ? new QName(this.typeNamespaceUri, name) : new QName(name));
	}

}
