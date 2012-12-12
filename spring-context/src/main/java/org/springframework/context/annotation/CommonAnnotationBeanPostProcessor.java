/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context.annotation;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceRef;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.jndi.support.SimpleJndiBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that supports common Java annotations out of the box, in particular the JSR-250
 * annotations in the <code>javax.annotation</code> package. These common Java
 * annotations are supported in many Java EE 5 technologies (e.g. JSF 1.2),
 * as well as in Java 6's JAX-WS.
 *
 * <p>This post-processor includes support for the {@link javax.annotation.PostConstruct}
 * and {@link javax.annotation.PreDestroy} annotations - as init annotation
 * and destroy annotation, respectively - through inheriting from
 * {@link InitDestroyAnnotationBeanPostProcessor} with pre-configured annotation types.
 *
 * <p>The central element is the {@link javax.annotation.Resource} annotation
 * for annotation-driven injection of named beans, by default from the containing
 * Spring BeanFactory, with only <code>mappedName</code> references resolved in JNDI.
 * The {@link #setAlwaysUseJndiLookup "alwaysUseJndiLookup" flag} enforces JNDI lookups
 * equivalent to standard Java EE 5 resource injection for <code>name</code> references
 * and default names as well. The target beans can be simple POJOs, with no special
 * requirements other than the type having to match.
 *
 * <p>The JAX-WS {@link javax.xml.ws.WebServiceRef} annotation is supported too,
 * analogous to {@link javax.annotation.Resource} but with the capability of creating
 * specific JAX-WS service endpoints. This may either point to an explicitly defined
 * resource by name or operate on a locally specified JAX-WS service class. Finally,
 * this post-processor also supports the EJB 3 {@link javax.ejb.EJB} annotation,
 * analogous to {@link javax.annotation.Resource} as well, with the capability to
 * specify both a local bean name and a global JNDI name for fallback retrieval.
 * The target beans can be plain POJOs as well as EJB 3 Session Beans in this case.
 *
 * <p>The common annotations supported by this post-processor are available in
 * Java 6 (JDK 1.6) as well as in Java EE 5/6 (which provides a standalone jar for
 * its common annotations as well, allowing for use in any Java 5 based application).
 * Hence, this post-processor works out of the box on JDK 1.6, and requires the
 * JSR-250 API jar (and optionally the JAX-WS API jar and/or the EJB 3 API jar)
 * to be added to the classpath on JDK 1.5 (when running outside of Java EE 5/6).
 *
 * <p>For default usage, resolving resource names as Spring bean names,
 * simply define the following in your application context:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.context.annotation.CommonAnnotationBeanPostProcessor"/&gt;</pre>
 *
 * For direct JNDI access, resolving resource names as JNDI resource references
 * within the Java EE application's "java:comp/env/" namespace, use the following:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.context.annotation.CommonAnnotationBeanPostProcessor"&gt;
 *   &lt;property name="alwaysUseJndiLookup" value="true"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <code>mappedName</code> references will always be resolved in JNDI,
 * allowing for global JNDI names (including "java:" prefix) as well. The
 * "alwaysUseJndiLookup" flag just affects <code>name</code> references and
 * default names (inferred from the field name / property name).
 *
 * <p><b>NOTE:</b> A default CommonAnnotationBeanPostProcessor will be registered
 * by the "context:annotation-config" and "context:component-scan" XML tags.
 * Remove or turn off the default annotation configuration there if you intend
 * to specify a custom CommonAnnotationBeanPostProcessor bean definition!
 * <p><b>NOTE:</b> Annotation injection will be performed <i>before</i> XML injection; thus
 * the latter configuration will override the former for properties wired through
 * both approaches. 
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setAlwaysUseJndiLookup
 * @see #setResourceFactory
 * @see org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor
 * @see org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
 */
@SuppressWarnings("serial")
public class CommonAnnotationBeanPostProcessor extends InitDestroyAnnotationBeanPostProcessor
		implements InstantiationAwareBeanPostProcessor, BeanFactoryAware, Serializable {

	private static Class<? extends Annotation> webServiceRefClass = null;

	private static Class<? extends Annotation> ejbRefClass = null;

	static {
		ClassLoader cl = CommonAnnotationBeanPostProcessor.class.getClassLoader();
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> clazz = (Class<? extends Annotation>) cl.loadClass("javax.xml.ws.WebServiceRef");
			webServiceRefClass = clazz;
		}
		catch (ClassNotFoundException ex) {
			webServiceRefClass = null;
		}
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> clazz = (Class<? extends Annotation>) cl.loadClass("javax.ejb.EJB");
			ejbRefClass = clazz;
		}
		catch (ClassNotFoundException ex) {
			ejbRefClass = null;
		}
	}


	private final Set<String> ignoredResourceTypes = new HashSet<String>(1);

	private boolean fallbackToDefaultTypeMatch = true;

	private boolean alwaysUseJndiLookup = false;

	private transient BeanFactory jndiFactory = new SimpleJndiBeanFactory();

	private transient BeanFactory resourceFactory;

	private transient BeanFactory beanFactory;

	private transient final Map<Class<?>, InjectionMetadata> injectionMetadataCache =
			new ConcurrentHashMap<Class<?>, InjectionMetadata>(64);


	/**
	 * Create a new CommonAnnotationBeanPostProcessor,
	 * with the init and destroy annotation types set to
	 * {@link javax.annotation.PostConstruct} and {@link javax.annotation.PreDestroy},
	 * respectively.
	 */
	public CommonAnnotationBeanPostProcessor() {
		setOrder(Ordered.LOWEST_PRECEDENCE - 3);
		setInitAnnotationType(PostConstruct.class);
		setDestroyAnnotationType(PreDestroy.class);
		ignoreResourceType("javax.xml.ws.WebServiceContext");
	}


	/**
	 * Ignore the given resource type when resolving <code>@Resource</code>
	 * annotations.
	 * <p>By default, the <code>javax.xml.ws.WebServiceContext</code> interface
	 * will be ignored, since it will be resolved by the JAX-WS runtime.
	 * @param resourceType the resource type to ignore
	 */
	public void ignoreResourceType(String resourceType) {
		Assert.notNull(resourceType, "Ignored resource type must not be null");
		this.ignoredResourceTypes.add(resourceType);
	}

	/**
	 * Set whether to allow a fallback to a type match if no explicit name has been
	 * specified. The default name (i.e. the field name or bean property name) will
	 * still be checked first; if a bean of that name exists, it will be taken.
	 * However, if no bean of that name exists, a by-type resolution of the
	 * dependency will be attempted if this flag is "true".
	 * <p>Default is "true". Switch this flag to "false" in order to enforce a
	 * by-name lookup in all cases, throwing an exception in case of no name match.
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#resolveDependency
	 */
	public void setFallbackToDefaultTypeMatch(boolean fallbackToDefaultTypeMatch) {
		this.fallbackToDefaultTypeMatch = fallbackToDefaultTypeMatch;
	}

	/**
	 * Set whether to always use JNDI lookups equivalent to standard Java EE 5 resource
	 * injection, <b>even for <code>name</code> attributes and default names</b>.
	 * <p>Default is "false": Resource names are used for Spring bean lookups in the
	 * containing BeanFactory; only <code>mappedName</code> attributes point directly
	 * into JNDI. Switch this flag to "true" for enforcing Java EE style JNDI lookups
	 * in any case, even for <code>name</code> attributes and default names.
	 * @see #setJndiFactory
	 * @see #setResourceFactory
	 */
	public void setAlwaysUseJndiLookup(boolean alwaysUseJndiLookup) {
		this.alwaysUseJndiLookup = alwaysUseJndiLookup;
	}

	/**
	 * Specify the factory for objects to be injected into <code>@Resource</code> /
	 * <code>@WebServiceRef</code> / <code>@EJB</code> annotated fields and setter methods,
	 * <b>for <code>mappedName</code> attributes that point directly into JNDI</b>.
	 * This factory will also be used if "alwaysUseJndiLookup" is set to "true" in order
	 * to enforce JNDI lookups even for <code>name</code> attributes and default names.
	 * <p>The default is a {@link org.springframework.jndi.support.SimpleJndiBeanFactory}
	 * for JNDI lookup behavior equivalent to standard Java EE 5 resource injection.
	 * @see #setResourceFactory
	 * @see #setAlwaysUseJndiLookup
	 */
	public void setJndiFactory(BeanFactory jndiFactory) {
		Assert.notNull(jndiFactory, "BeanFactory must not be null");
		this.jndiFactory = jndiFactory;
	}

	/**
	 * Specify the factory for objects to be injected into <code>@Resource</code> /
	 * <code>@WebServiceRef</code> / <code>@EJB</code> annotated fields and setter methods,
	 * <b>for <code>name</code> attributes and default names</b>.
	 * <p>The default is the BeanFactory that this post-processor is defined in,
	 * if any, looking up resource names as Spring bean names. Specify the resource
	 * factory explicitly for programmatic usage of this post-processor.
	 * <p>Specifying Spring's {@link org.springframework.jndi.support.SimpleJndiBeanFactory}
	 * leads to JNDI lookup behavior equivalent to standard Java EE 5 resource injection,
	 * even for <code>name</code> attributes and default names. This is the same behavior
	 * that the "alwaysUseJndiLookup" flag enables.
	 * @see #setAlwaysUseJndiLookup
	 */
	public void setResourceFactory(BeanFactory resourceFactory) {
		Assert.notNull(resourceFactory, "BeanFactory must not be null");
		this.resourceFactory = resourceFactory;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		if (this.resourceFactory == null) {
			this.resourceFactory = beanFactory;
		}
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		super.postProcessMergedBeanDefinition(beanDefinition, beanType, beanName);
		if (beanType != null) {
			InjectionMetadata metadata = findResourceMetadata(beanType);
			metadata.checkConfigMembers(beanDefinition);
		}
	}

	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		InjectionMetadata metadata = findResourceMetadata(bean.getClass());
		try {
			metadata.inject(bean, beanName, pvs);
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Injection of resource dependencies failed", ex);
		}
		return pvs;
	}


	private InjectionMetadata findResourceMetadata(final Class<?> clazz) {
		// Quick check on the concurrent map first, with minimal locking.
		InjectionMetadata metadata = this.injectionMetadataCache.get(clazz);
		if (metadata == null) {
			synchronized (this.injectionMetadataCache) {
				metadata = this.injectionMetadataCache.get(clazz);
				if (metadata == null) {
					LinkedList<InjectionMetadata.InjectedElement> elements = new LinkedList<InjectionMetadata.InjectedElement>();
					Class<?> targetClass = clazz;

					do {
						LinkedList<InjectionMetadata.InjectedElement> currElements = new LinkedList<InjectionMetadata.InjectedElement>();
						for (Field field : targetClass.getDeclaredFields()) {
							if (webServiceRefClass != null && field.isAnnotationPresent(webServiceRefClass)) {
								if (Modifier.isStatic(field.getModifiers())) {
									throw new IllegalStateException("@WebServiceRef annotation is not supported on static fields");
								}
								currElements.add(new WebServiceRefElement(field, null));
							}
							else if (ejbRefClass != null && field.isAnnotationPresent(ejbRefClass)) {
								if (Modifier.isStatic(field.getModifiers())) {
									throw new IllegalStateException("@EJB annotation is not supported on static fields");
								}
								currElements.add(new EjbRefElement(field, null));
							}
							else if (field.isAnnotationPresent(Resource.class)) {
								if (Modifier.isStatic(field.getModifiers())) {
									throw new IllegalStateException("@Resource annotation is not supported on static fields");
								}
								if (!ignoredResourceTypes.contains(field.getType().getName())) {
									currElements.add(new ResourceElement(field, null));
								}
							}
						}
						for (Method method : targetClass.getDeclaredMethods()) {
							method = BridgeMethodResolver.findBridgedMethod(method);
							Method mostSpecificMethod = BridgeMethodResolver.findBridgedMethod(ClassUtils.getMostSpecificMethod(method, clazz));
							if (method.equals(mostSpecificMethod)) {
								if (webServiceRefClass != null && method.isAnnotationPresent(webServiceRefClass)) {
									if (Modifier.isStatic(method.getModifiers())) {
										throw new IllegalStateException("@WebServiceRef annotation is not supported on static methods");
									}
									if (method.getParameterTypes().length != 1) {
										throw new IllegalStateException("@WebServiceRef annotation requires a single-arg method: " + method);
									}
									PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
									currElements.add(new WebServiceRefElement(method, pd));
								}
								else if (ejbRefClass != null && method.isAnnotationPresent(ejbRefClass)) {
									if (Modifier.isStatic(method.getModifiers())) {
										throw new IllegalStateException("@EJB annotation is not supported on static methods");
									}
									if (method.getParameterTypes().length != 1) {
										throw new IllegalStateException("@EJB annotation requires a single-arg method: " + method);
									}
									PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
									currElements.add(new EjbRefElement(method, pd));
								}
								else if (method.isAnnotationPresent(Resource.class)) {
									if (Modifier.isStatic(method.getModifiers())) {
										throw new IllegalStateException("@Resource annotation is not supported on static methods");
									}
									Class<?>[] paramTypes = method.getParameterTypes();
									if (paramTypes.length != 1) {
										throw new IllegalStateException("@Resource annotation requires a single-arg method: " + method);
									}
									if (!ignoredResourceTypes.contains(paramTypes[0].getName())) {
										PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
										currElements.add(new ResourceElement(method, pd));
									}
								}
							}
						}
						elements.addAll(0, currElements);
						targetClass = targetClass.getSuperclass();
					}
					while (targetClass != null && targetClass != Object.class);

					metadata = new InjectionMetadata(clazz, elements);
					this.injectionMetadataCache.put(clazz, metadata);
				}
			}
		}
		return metadata;
	}

	/**
	 * Obtain the resource object for the given name and type.
	 * @param element the descriptor for the annotated field/method
	 * @param requestingBeanName the name of the requesting bean
	 * @return the resource object (never <code>null</code>)
	 * @throws BeansException if we failed to obtain the target resource
	 */
	protected Object getResource(LookupElement element, String requestingBeanName) throws BeansException {
		if (StringUtils.hasLength(element.mappedName)) {
			return this.jndiFactory.getBean(element.mappedName, element.lookupType);
		}
		if (this.alwaysUseJndiLookup) {
			return this.jndiFactory.getBean(element.name, element.lookupType);
		}
		if (this.resourceFactory == null) {
			throw new NoSuchBeanDefinitionException(element.lookupType,
					"No resource factory configured - specify the 'resourceFactory' property");
		}
		return autowireResource(this.resourceFactory, element, requestingBeanName);
	}

	/**
	 * Obtain a resource object for the given name and type through autowiring
	 * based on the given factory.
	 * @param factory the factory to autowire against
	 * @param element the descriptor for the annotated field/method
	 * @param requestingBeanName the name of the requesting bean
	 * @return the resource object (never <code>null</code>)
	 * @throws BeansException if we failed to obtain the target resource
	 */
	protected Object autowireResource(BeanFactory factory, LookupElement element, String requestingBeanName)
			throws BeansException {

		Object resource;
		Set<String> autowiredBeanNames;
		String name = element.name;

		if (this.fallbackToDefaultTypeMatch && element.isDefaultName &&
				factory instanceof AutowireCapableBeanFactory && !factory.containsBean(name)) {
			autowiredBeanNames = new LinkedHashSet<String>();
			resource = ((AutowireCapableBeanFactory) factory).resolveDependency(
					element.getDependencyDescriptor(), requestingBeanName, autowiredBeanNames, null);
		}
		else {
			resource = factory.getBean(name, element.lookupType);
			autowiredBeanNames = Collections.singleton(name);
		}

		if (factory instanceof ConfigurableBeanFactory) {
			ConfigurableBeanFactory beanFactory = (ConfigurableBeanFactory) factory;
			for (String autowiredBeanName : autowiredBeanNames) {
				if (beanFactory.containsBean(autowiredBeanName)) {
					beanFactory.registerDependentBean(autowiredBeanName, requestingBeanName);
				}
			}
		}

		return resource;
	}


	/**
	 * Class representing generic injection information about an annotated field
	 * or setter method, supporting @Resource and related annotations.
	 */
	protected abstract class LookupElement extends InjectionMetadata.InjectedElement {

		protected String name;

		protected boolean isDefaultName = false;

		protected Class<?> lookupType;

		protected String mappedName;

		public LookupElement(Member member, PropertyDescriptor pd) {
			super(member, pd);
			initAnnotation((AnnotatedElement) member);
		}

		protected abstract void initAnnotation(AnnotatedElement ae);

		/**
		 * Return the resource name for the lookup.
		 */
		public final String getName() {
			return this.name;
		}

		/**
		 * Return the desired type for the lookup.
		 */
		public final Class<?> getLookupType() {
			return this.lookupType;
		}

		/**
		 * Build a DependencyDescriptor for the underlying field/method.
		 */
		public final DependencyDescriptor getDependencyDescriptor() {
			if (this.isField) {
				return new LookupDependencyDescriptor((Field) this.member, this.lookupType);
			}
			else {
				return new LookupDependencyDescriptor((Method) this.member, this.lookupType);
			}
		}
	}


	/**
	 * Class representing injection information about an annotated field
	 * or setter method, supporting the @Resource annotation.
	 */
	private class ResourceElement extends LookupElement {

		protected boolean shareable = true;

		public ResourceElement(Member member, PropertyDescriptor pd) {
			super(member, pd);
		}

		@Override
		protected void initAnnotation(AnnotatedElement ae) {
			Resource resource = ae.getAnnotation(Resource.class);
			String resourceName = resource.name();
			Class<?> resourceType = resource.type();
			this.isDefaultName = !StringUtils.hasLength(resourceName);
			if (this.isDefaultName) {
				resourceName = this.member.getName();
				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
					resourceName = Introspector.decapitalize(resourceName.substring(3));
				}
			}
			else if (beanFactory instanceof ConfigurableBeanFactory){
				resourceName = ((ConfigurableBeanFactory) beanFactory).resolveEmbeddedValue(resourceName);
			}
			if (resourceType != null && !Object.class.equals(resourceType)) {
				checkResourceType(resourceType);
			}
			else {
				// No resource type specified... check field/method.
				resourceType = getResourceType();
			}
			this.name = resourceName;
			this.lookupType = resourceType;
			this.mappedName = resource.mappedName();
			this.shareable = resource.shareable();
		}

		@Override
		protected Object getResourceToInject(Object target, String requestingBeanName) {
			return getResource(this, requestingBeanName);
		}
	}


	/**
	 * Class representing injection information about an annotated field
	 * or setter method, supporting the @WebServiceRef annotation.
	 */
	private class WebServiceRefElement extends LookupElement {

		private Class<?> elementType;

		private String wsdlLocation;

		public WebServiceRefElement(Member member, PropertyDescriptor pd) {
			super(member, pd);
		}

		@Override
		protected void initAnnotation(AnnotatedElement ae) {
			WebServiceRef resource = ae.getAnnotation(WebServiceRef.class);
			String resourceName = resource.name();
			Class<?> resourceType = resource.type();
			this.isDefaultName = !StringUtils.hasLength(resourceName);
			if (this.isDefaultName) {
				resourceName = this.member.getName();
				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
					resourceName = Introspector.decapitalize(resourceName.substring(3));
				}
			}
			if (resourceType != null && !Object.class.equals(resourceType)) {
				checkResourceType(resourceType);
			}
			else {
				// No resource type specified... check field/method.
				resourceType = getResourceType();
			}
			this.name = resourceName;
			this.elementType = resourceType;
			if (Service.class.isAssignableFrom(resourceType)) {
				this.lookupType = resourceType;
			}
			else {
				this.lookupType = (!Object.class.equals(resource.value()) ? resource.value() : Service.class);
			}
			this.mappedName = resource.mappedName();
			this.wsdlLocation = resource.wsdlLocation();
		}

		@Override
		protected Object getResourceToInject(Object target, String requestingBeanName) {
			Service service;
			try {
				service = (Service) getResource(this, requestingBeanName);
			}
			catch (NoSuchBeanDefinitionException notFound) {
				// Service to be created through generated class.
				if (Service.class.equals(this.lookupType)) {
					throw new IllegalStateException("No resource with name '" + this.name + "' found in context, " +
							"and no specific JAX-WS Service subclass specified. The typical solution is to either specify " +
							"a LocalJaxWsServiceFactoryBean with the given name or to specify the (generated) Service " +
							"subclass as @WebServiceRef(...) value.");
				}
				if (StringUtils.hasLength(this.wsdlLocation)) {
					try {
						Constructor<?> ctor = this.lookupType.getConstructor(new Class[] {URL.class, QName.class});
						WebServiceClient clientAnn = this.lookupType.getAnnotation(WebServiceClient.class);
						if (clientAnn == null) {
							throw new IllegalStateException("JAX-WS Service class [" + this.lookupType.getName() +
									"] does not carry a WebServiceClient annotation");
						}
						service = (Service) BeanUtils.instantiateClass(ctor,
								new URL(this.wsdlLocation), new QName(clientAnn.targetNamespace(), clientAnn.name()));
					}
					catch (NoSuchMethodException ex) {
						throw new IllegalStateException("JAX-WS Service class [" + this.lookupType.getName() +
								"] does not have a (URL, QName) constructor. Cannot apply specified WSDL location [" +
								this.wsdlLocation + "].");
					}
					catch (MalformedURLException ex) {
						throw new IllegalArgumentException(
								"Specified WSDL location [" + this.wsdlLocation + "] isn't a valid URL");
					}
				}
				else {
					service = (Service) BeanUtils.instantiateClass(this.lookupType);
				}
			}
			return service.getPort(this.elementType);
		}
	}


	/**
	 * Class representing injection information about an annotated field
	 * or setter method, supporting the @EJB annotation.
	 */
	private class EjbRefElement extends LookupElement {

		private String beanName;

		public EjbRefElement(Member member, PropertyDescriptor pd) {
			super(member, pd);
		}

		@Override
		protected void initAnnotation(AnnotatedElement ae) {
			EJB resource = ae.getAnnotation(EJB.class);
			String resourceBeanName = resource.beanName();
			String resourceName = resource.name();
			this.isDefaultName = !StringUtils.hasLength(resourceName);
			if (this.isDefaultName) {
				resourceName = this.member.getName();
				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
					resourceName = Introspector.decapitalize(resourceName.substring(3));
				}
			}
			Class<?> resourceType = resource.beanInterface();
			if (resourceType != null && !Object.class.equals(resourceType)) {
				checkResourceType(resourceType);
			}
			else {
				// No resource type specified... check field/method.
				resourceType = getResourceType();
			}
			this.beanName = resourceBeanName;
			this.name = resourceName;
			this.lookupType = resourceType;
			this.mappedName = resource.mappedName();
		}

		@Override
		protected Object getResourceToInject(Object target, String requestingBeanName) {
			if (StringUtils.hasLength(this.beanName)) {
				if (beanFactory != null && beanFactory.containsBean(this.beanName)) {
					// Local match found for explicitly specified local bean name.
					Object bean = beanFactory.getBean(this.beanName, this.lookupType);
					if (beanFactory instanceof ConfigurableBeanFactory) {
						((ConfigurableBeanFactory) beanFactory).registerDependentBean(this.beanName, requestingBeanName);
					}
					return bean;
				}
				else if (this.isDefaultName && !StringUtils.hasLength(this.mappedName)) {
					throw new NoSuchBeanDefinitionException(this.beanName,
							"Cannot resolve 'beanName' in local BeanFactory. Consider specifying a general 'name' value instead.");
				}
			}
			// JNDI name lookup - may still go to a local BeanFactory.
			return getResource(this, requestingBeanName);
		}
	}


	/**
	 * Extension of the DependencyDescriptor class,
	 * overriding the dependency type with the specified resource type.
	 */
	private static class LookupDependencyDescriptor extends DependencyDescriptor {

		private final Class<?> lookupType;

		public LookupDependencyDescriptor(Field field, Class<?> lookupType) {
			super(field, true);
			this.lookupType = lookupType;
		}

		public LookupDependencyDescriptor(Method method, Class<?> lookupType) {
			super(new MethodParameter(method, 0), true);
			this.lookupType = lookupType;
		}

		@Override
		public Class<?> getDependencyType() {
			return this.lookupType;
		}
	}

}