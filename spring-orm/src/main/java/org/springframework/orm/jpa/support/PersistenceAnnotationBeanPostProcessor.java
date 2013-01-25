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

package org.springframework.orm.jpa.support;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;
import javax.persistence.PersistenceUnit;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiTemplate;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerProxy;
import org.springframework.orm.jpa.ExtendedEntityManagerCreator;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * BeanPostProcessor that processes {@link javax.persistence.PersistenceUnit}
 * and {@link javax.persistence.PersistenceContext} annotations, for injection of
 * the corresponding JPA resources {@link javax.persistence.EntityManagerFactory}
 * and {@link javax.persistence.EntityManager}. Any such annotated fields or methods
 * in any Spring-managed object will automatically be injected.
 *
 * <p>This post-processor will inject sub-interfaces of {@code EntityManagerFactory}
 * and {@code EntityManager} if the annotated fields or methods are declared as such.
 * The actual type will be verified early, with the exception of a shared ("transactional")
 * {@code EntityManager} reference, where type mismatches might be detected as late
 * as on the first actual invocation.
 *
 * <p>Note: In the present implementation, PersistenceAnnotationBeanPostProcessor
 * only supports {@code @PersistenceUnit} and {@code @PersistenceContext}
 * with the "unitName" attribute, or no attribute at all (for the default unit).
 * If those annotations are present with the "name" attribute at the class level,
 * they will simply be ignored, since those only serve as deployment hint
 * (as per the Java EE 5 specification).
 *
 * <p>This post-processor can either obtain EntityManagerFactory beans defined
 * in the Spring application context (the default), or obtain EntityManagerFactory
 * references from JNDI ("persistence unit references"). In the bean case,
 * the persistence unit name will be matched against the actual deployed unit,
 * with the bean name used as fallback unit name if no deployed name found.
 * Typically, Spring's {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * will be used for setting up such EntityManagerFactory beans. Alternatively,
 * such beans may also be obtained from JNDI, e.g. using the {@code jee:jndi-lookup}
 * XML configuration element (with the bean name matching the requested unit name).
 * In both cases, the post-processor definition will look as simple as this:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor"/&gt;</pre>
 *
 * In the JNDI case, specify the corresponding JNDI names in this post-processor's
 * {@link #setPersistenceUnits "persistenceUnits" map}, typically with matching
 * {@code persistence-unit-ref} entries in the Java EE deployment descriptor.
 * By default, those names are considered as resource references (according to the
 * Java EE resource-ref convention), located underneath the "java:comp/env/" namespace.
 * For example:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor"&gt;
 *   &lt;property name="persistenceUnits"&gt;
 *     &lt;map/gt;
 *       &lt;entry key="unit1" value="persistence/unit1"/&gt;
 *       &lt;entry key="unit2" value="persistence/unit2"/&gt;
 *     &lt;/map/gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * In this case, the specified persistence units will always be resolved in JNDI
 * rather than as Spring-defined beans. The entire persistence unit deployment,
 * including the weaving of persistent classes, is then up to the Java EE server.
 * Persistence contexts (i.e. EntityManager references) will be built based on
 * those server-provided EntityManagerFactory references, using Spring's own
 * transaction synchronization facilities for transactional EntityManager handling
 * (typically with Spring's {@code @Transactional} annotation for demarcation
 * and {@link org.springframework.transaction.jta.JtaTransactionManager} as backend).
 *
 * <p>If you prefer the Java EE server's own EntityManager handling, specify entries
 * in this post-processor's {@link #setPersistenceContexts "persistenceContexts" map}
 * (or {@link #setExtendedPersistenceContexts "extendedPersistenceContexts" map},
 * typically with matching {@code persistence-context-ref} entries in the
 * Java EE deployment descriptor. For example:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor"&gt;
 *   &lt;property name="persistenceContexts"&gt;
 *     &lt;map/gt;
 *       &lt;entry key="unit1" value="persistence/context1"/&gt;
 *       &lt;entry key="unit2" value="persistence/context2"/&gt;
 *     &lt;/map/gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * If the application only obtains EntityManager references in the first place,
 * this is all you need to specify. If you need EntityManagerFactory references
 * as well, specify entries for both "persistenceUnits" and "persistenceContexts",
 * pointing to matching JNDI locations.
 *
 * <p><b>NOTE: In general, do not inject EXTENDED EntityManagers into STATELESS beans,
 * i.e. do not use {@code @PersistenceContext} with type {@code EXTENDED} in
 * Spring beans defined with scope 'singleton' (Spring's default scope).</b>
 * Extended EntityManagers are <i>not</i> thread-safe, hence they must not be used
 * in concurrently accessed beans (which Spring-managed singletons usually are).
 *
 * <p>Note: A default PersistenceAnnotationBeanPostProcessor will be registered
 * by the "context:annotation-config" and "context:component-scan" XML tags.
 * Remove or turn off the default annotation configuration there if you intend
 * to specify a custom PersistenceAnnotationBeanPostProcessor bean definition.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see javax.persistence.PersistenceUnit
 * @see javax.persistence.PersistenceContext
 */
@SuppressWarnings("serial")
public class PersistenceAnnotationBeanPostProcessor
		implements InstantiationAwareBeanPostProcessor, DestructionAwareBeanPostProcessor,
		MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware, Serializable {

	private Object jndiEnvironment;

	private boolean resourceRef = true;

	private transient Map<String, String> persistenceUnits;

	private transient Map<String, String> persistenceContexts;

	private transient Map<String, String> extendedPersistenceContexts;

	private transient String defaultPersistenceUnitName = "";

	private int order = Ordered.LOWEST_PRECEDENCE - 4;

	private transient ListableBeanFactory beanFactory;

	private transient final Map<Class<?>, InjectionMetadata> injectionMetadataCache =
			new ConcurrentHashMap<Class<?>, InjectionMetadata>(64);

	private final Map<Object, EntityManager> extendedEntityManagersToClose =
			new ConcurrentHashMap<Object, EntityManager>(16);


	/**
	 * Set the JNDI template to use for JNDI lookups.
	 * @see org.springframework.jndi.JndiAccessor#setJndiTemplate
	 */
	public void setJndiTemplate(Object jndiTemplate) {
		this.jndiEnvironment = jndiTemplate;
	}

	/**
	 * Set the JNDI environment to use for JNDI lookups.
	 * @see org.springframework.jndi.JndiAccessor#setJndiEnvironment
	 */
	public void setJndiEnvironment(Properties jndiEnvironment) {
		this.jndiEnvironment = jndiEnvironment;
	}

	/**
	 * Set whether the lookup occurs in a J2EE container, i.e. if the prefix
	 * "java:comp/env/" needs to be added if the JNDI name doesn't already
	 * contain it. PersistenceAnnotationBeanPostProcessor's default is "true".
	 * @see org.springframework.jndi.JndiLocatorSupport#setResourceRef
	 */
	public void setResourceRef(boolean resourceRef) {
		this.resourceRef = resourceRef;
	}

	/**
	 * Specify the persistence units for EntityManagerFactory lookups,
	 * as a Map from persistence unit name to persistence unit JNDI name
	 * (which needs to resolve to an EntityManagerFactory instance).
	 * <p>JNDI names specified here should refer to {@code persistence-unit-ref}
	 * entries in the Java EE deployment descriptor, matching the target persistence unit.
	 * <p>In case of no unit name specified in the annotation, the specified value
	 * for the {@link #setDefaultPersistenceUnitName default persistence unit}
	 * will be taken (by default, the value mapped to the empty String),
	 * or simply the single persistence unit if there is only one.
	 * <p>This is mainly intended for use in a Java EE 5 environment, with all
	 * lookup driven by the standard JPA annotations, and all EntityManagerFactory
	 * references obtained from JNDI. No separate EntityManagerFactory bean
	 * definitions are necessary in such a scenario.
	 * <p>If no corresponding "persistenceContexts"/"extendedPersistenceContexts"
	 * are specified, {@code @PersistenceContext} will be resolved to
	 * EntityManagers built on top of the EntityManagerFactory defined here.
	 * Note that those will be Spring-managed EntityManagers, which implement
	 * transaction synchronization based on Spring's facilities.
	 * If you prefer the Java EE 5 server's own EntityManager handling,
	 * specify corresponding "persistenceContexts"/"extendedPersistenceContexts".
	 */
	public void setPersistenceUnits(Map<String, String> persistenceUnits) {
		this.persistenceUnits = persistenceUnits;
	}

	/**
	 * Specify the <i>transactional</i> persistence contexts for EntityManager lookups,
	 * as a Map from persistence unit name to persistence context JNDI name
	 * (which needs to resolve to an EntityManager instance).
	 * <p>JNDI names specified here should refer to {@code persistence-context-ref}
	 * entries in the Java EE deployment descriptors, matching the target persistence unit
	 * and being set up with persistence context type {@code Transaction}.
	 * <p>In case of no unit name specified in the annotation, the specified value
	 * for the {@link #setDefaultPersistenceUnitName default persistence unit}
	 * will be taken (by default, the value mapped to the empty String),
	 * or simply the single persistence unit if there is only one.
	 * <p>This is mainly intended for use in a Java EE 5 environment, with all
	 * lookup driven by the standard JPA annotations, and all EntityManager
	 * references obtained from JNDI. No separate EntityManagerFactory bean
	 * definitions are necessary in such a scenario, and all EntityManager
	 * handling is done by the Java EE 5 server itself.
	 */
	public void setPersistenceContexts(Map<String, String> persistenceContexts) {
		this.persistenceContexts = persistenceContexts;
	}

	/**
	 * Specify the <i>extended</i> persistence contexts for EntityManager lookups,
	 * as a Map from persistence unit name to persistence context JNDI name
	 * (which needs to resolve to an EntityManager instance).
	 * <p>JNDI names specified here should refer to {@code persistence-context-ref}
	 * entries in the Java EE deployment descriptors, matching the target persistence unit
	 * and being set up with persistence context type {@code Extended}.
	 * <p>In case of no unit name specified in the annotation, the specified value
	 * for the {@link #setDefaultPersistenceUnitName default persistence unit}
	 * will be taken (by default, the value mapped to the empty String),
	 * or simply the single persistence unit if there is only one.
	 * <p>This is mainly intended for use in a Java EE 5 environment, with all
	 * lookup driven by the standard JPA annotations, and all EntityManager
	 * references obtained from JNDI. No separate EntityManagerFactory bean
	 * definitions are necessary in such a scenario, and all EntityManager
	 * handling is done by the Java EE 5 server itself.
	 */
	public void setExtendedPersistenceContexts(Map<String, String> extendedPersistenceContexts) {
		this.extendedPersistenceContexts = extendedPersistenceContexts;
	}

	/**
	 * Specify the default persistence unit name, to be used in case
	 * of no unit name specified in an {@code @PersistenceUnit} /
	 * {@code @PersistenceContext} annotation.
	 * <p>This is mainly intended for lookups in the application context,
	 * indicating the target persistence unit name (typically matching
	 * the bean name), but also applies to lookups in the
	 * {@link #setPersistenceUnits "persistenceUnits"} /
	 * {@link #setPersistenceContexts "persistenceContexts"} /
	 * {@link #setExtendedPersistenceContexts "extendedPersistenceContexts"} map,
	 * avoiding the need for duplicated mappings for the empty String there.
	 * <p>Default is to check for a single EntityManagerFactory bean
	 * in the Spring application context, if any. If there are multiple
	 * such factories, either specify this default persistence unit name
	 * or explicitly refer to named persistence units in your annotations.
	 */
	public void setDefaultPersistenceUnitName(String unitName) {
		this.defaultPersistenceUnitName = (unitName != null ? unitName : "");
	}

	public void setOrder(int order) {
	  this.order = order;
	}

	public int getOrder() {
	  return this.order;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ListableBeanFactory) {
			this.beanFactory = (ListableBeanFactory) beanFactory;
		}
	}


	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		if (beanType != null) {
			InjectionMetadata metadata = findPersistenceMetadata(beanType);
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

		InjectionMetadata metadata = findPersistenceMetadata(bean.getClass());
		try {
			metadata.inject(bean, beanName, pvs);
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Injection of persistence dependencies failed", ex);
		}
		return pvs;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		EntityManager emToClose = this.extendedEntityManagersToClose.remove(bean);
		EntityManagerFactoryUtils.closeEntityManager(emToClose);
	}


	private InjectionMetadata findPersistenceMetadata(final Class<?> clazz) {
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
							PersistenceContext pc = field.getAnnotation(PersistenceContext.class);
							PersistenceUnit pu = field.getAnnotation(PersistenceUnit.class);
							if (pc != null || pu != null) {
								if (Modifier.isStatic(field.getModifiers())) {
									throw new IllegalStateException("Persistence annotations are not supported on static fields");
								}
								currElements.add(new PersistenceElement(field, null));
							}
						}
						for (Method method : targetClass.getDeclaredMethods()) {
							PersistenceContext pc = method.getAnnotation(PersistenceContext.class);
							PersistenceUnit pu = method.getAnnotation(PersistenceUnit.class);
							if ((pc != null || pu != null) &&
									method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
								if (Modifier.isStatic(method.getModifiers())) {
									throw new IllegalStateException("Persistence annotations are not supported on static methods");
								}
								if (method.getParameterTypes().length != 1) {
									throw new IllegalStateException("Persistence annotation requires a single-arg method: " + method);
								}
								PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
								currElements.add(new PersistenceElement(method, pd));
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
	 * Return a specified persistence unit for the given unit name,
	 * as defined through the "persistenceUnits" map.
	 * @param unitName the name of the persistence unit
	 * @return the corresponding EntityManagerFactory,
	 * or {@code null} if none found
	 * @see #setPersistenceUnits
	 */
	protected EntityManagerFactory getPersistenceUnit(String unitName) {
		if (this.persistenceUnits != null) {
			String unitNameForLookup = (unitName != null ? unitName : "");
			if ("".equals(unitNameForLookup)) {
				unitNameForLookup = this.defaultPersistenceUnitName;
			}
			String jndiName = this.persistenceUnits.get(unitNameForLookup);
			if (jndiName == null && "".equals(unitNameForLookup) && this.persistenceUnits.size() == 1) {
				jndiName = this.persistenceUnits.values().iterator().next();
			}
			if (jndiName != null) {
				try {
					return lookup(jndiName, EntityManagerFactory.class);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Could not obtain EntityManagerFactory [" + jndiName + "] from JNDI", ex);
				}
			}
		}
		return null;
	}

	/**
	 * Return a specified persistence context for the given unit name, as defined
	 * through the "persistenceContexts" (or "extendedPersistenceContexts") map.
	 * @param unitName the name of the persistence unit
	 * @param extended whether to obtain an extended persistence context
	 * @return the corresponding EntityManager, or {@code null} if none found
	 * @see #setPersistenceContexts
	 * @see #setExtendedPersistenceContexts
	 */
	protected EntityManager getPersistenceContext(String unitName, boolean extended) {
		Map<String, String> contexts = (extended ? this.extendedPersistenceContexts : this.persistenceContexts);
		if (contexts != null) {
			String unitNameForLookup = (unitName != null ? unitName : "");
			if ("".equals(unitNameForLookup)) {
				unitNameForLookup = this.defaultPersistenceUnitName;
			}
			String jndiName = contexts.get(unitNameForLookup);
			if (jndiName == null && "".equals(unitNameForLookup) && contexts.size() == 1) {
				jndiName = contexts.values().iterator().next();
			}
			if (jndiName != null) {
				try {
					return lookup(jndiName, EntityManager.class);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Could not obtain EntityManager [" + jndiName + "] from JNDI", ex);
				}
			}
		}
		return null;
	}

	/**
	 * Find an EntityManagerFactory with the given name in the current Spring
	 * application context, falling back to a single default EntityManagerFactory
	 * (if any) in case of no unit name specified.
	 * @param unitName the name of the persistence unit (may be {@code null} or empty)
	 * @param requestingBeanName the name of the requesting bean
	 * @return the EntityManagerFactory
	 * @throws NoSuchBeanDefinitionException if there is no such EntityManagerFactory in the context
	 */
	protected EntityManagerFactory findEntityManagerFactory(String unitName, String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		if (this.beanFactory == null) {
			throw new IllegalStateException("ListableBeanFactory required for EntityManagerFactory bean lookup");
		}
		String unitNameForLookup = (unitName != null ? unitName : "");
		if ("".equals(unitNameForLookup)) {
			unitNameForLookup = this.defaultPersistenceUnitName;
		}
		if (!"".equals(unitNameForLookup)) {
			return findNamedEntityManagerFactory(unitNameForLookup, requestingBeanName);
		}
		else {
			return findDefaultEntityManagerFactory(requestingBeanName);
		}
	}

	/**
	 * Find an EntityManagerFactory with the given name in the current
	 * Spring application context.
	 * @param unitName the name of the persistence unit (never empty)
	 * @param requestingBeanName the name of the requesting bean
	 * @return the EntityManagerFactory
	 * @throws NoSuchBeanDefinitionException if there is no such EntityManagerFactory in the context
	 */
	protected EntityManagerFactory findNamedEntityManagerFactory(String unitName, String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		EntityManagerFactory emf = EntityManagerFactoryUtils.findEntityManagerFactory(this.beanFactory, unitName);
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			((ConfigurableBeanFactory) this.beanFactory).registerDependentBean(unitName, requestingBeanName);
		}
		return emf;
	}

	/**
	 * Find a single default EntityManagerFactory in the Spring application context.
	 * @return the default EntityManagerFactory
	 * @throws NoSuchBeanDefinitionException if there is no single EntityManagerFactory in the context
	 */
	protected EntityManagerFactory findDefaultEntityManagerFactory(String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		String[] beanNames =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, EntityManagerFactory.class);
		if (beanNames.length == 1) {
			String unitName = beanNames[0];
			EntityManagerFactory emf = (EntityManagerFactory) this.beanFactory.getBean(unitName);
			if (this.beanFactory instanceof ConfigurableBeanFactory) {
				((ConfigurableBeanFactory) this.beanFactory).registerDependentBean(unitName, requestingBeanName);
			}
			return emf;
		}
		else if (beanNames.length > 1) {
			throw new NoUniqueBeanDefinitionException(EntityManagerFactory.class, beanNames);
		}
		else {
			throw new NoSuchBeanDefinitionException(EntityManagerFactory.class);
		}
	}

	/**
	 * Perform a JNDI lookup for the given resource by name.
	 * <p>Called for EntityManagerFactory and EntityManager lookup
	 * when JNDI names are mapped for specific persistence units.
	 * @param jndiName the JNDI name to look up
	 * @param requiredType the required type of the object
	 * @return the obtained object
	 * @throws Exception if the JNDI lookup failed
	 */
	protected <T> T lookup(String jndiName, Class<T> requiredType) throws Exception {
		return new LocatorDelegate().lookup(jndiName, requiredType);
	}


	/**
	 * Separate inner class to isolate the JNDI API dependency
	 * (for compatibility with Google App Engine's API white list).
	 */
	private class LocatorDelegate {

		public <T> T lookup(String jndiName, Class<T> requiredType) throws Exception {
			JndiLocatorDelegate locator = new JndiLocatorDelegate();
			if (jndiEnvironment instanceof JndiTemplate) {
				locator.setJndiTemplate((JndiTemplate) jndiEnvironment);
			}
			else if (jndiEnvironment instanceof Properties) {
				locator.setJndiEnvironment((Properties) jndiEnvironment);
			}
			else if (jndiEnvironment != null) {
				throw new IllegalStateException("Illegal 'jndiEnvironment' type: " + jndiEnvironment.getClass());
			}
			locator.setResourceRef(resourceRef);
			return locator.lookup(jndiName, requiredType);
		}
	}


	/**
	 * Class representing injection information about an annotated field
	 * or setter method.
	 */
	private class PersistenceElement extends InjectionMetadata.InjectedElement {

		private final String unitName;

		private PersistenceContextType type;

		private Properties properties;

		public PersistenceElement(Member member, PropertyDescriptor pd) {
			super(member, pd);
			AnnotatedElement ae = (AnnotatedElement) member;
			PersistenceContext pc = ae.getAnnotation(PersistenceContext.class);
			PersistenceUnit pu = ae.getAnnotation(PersistenceUnit.class);
			Class<?> resourceType = EntityManager.class;
			if (pc != null) {
				if (pu != null) {
					throw new IllegalStateException("Member may only be annotated with either " +
							"@PersistenceContext or @PersistenceUnit, not both: " + member);
				}
				Properties properties = null;
				PersistenceProperty[] pps = pc.properties();
				if (!ObjectUtils.isEmpty(pps)) {
					properties = new Properties();
					for (PersistenceProperty pp : pps) {
						properties.setProperty(pp.name(), pp.value());
					}
				}
				this.unitName = pc.unitName();
				this.type = pc.type();
				this.properties = properties;
			}
			else {
				resourceType = EntityManagerFactory.class;
				this.unitName = pu.unitName();
			}
			checkResourceType(resourceType);
		}

		/**
		 * Resolve the object against the application context.
		 */
		@Override
		protected Object getResourceToInject(Object target, String requestingBeanName) {
			// Resolves to EntityManagerFactory or EntityManager.
			if (this.type != null) {
				return (this.type == PersistenceContextType.EXTENDED ?
						resolveExtendedEntityManager(target, requestingBeanName) :
						resolveEntityManager(requestingBeanName));
			}
			else {
				// OK, so we need an EntityManagerFactory...
				return resolveEntityManagerFactory(requestingBeanName);
			}
		}

		private EntityManagerFactory resolveEntityManagerFactory(String requestingBeanName) {
			// Obtain EntityManagerFactory from JNDI?
			EntityManagerFactory emf = getPersistenceUnit(this.unitName);
			if (emf == null) {
				// Need to search for EntityManagerFactory beans.
				emf = findEntityManagerFactory(this.unitName, requestingBeanName);
			}
			return emf;
		}

		private EntityManager resolveEntityManager(String requestingBeanName) {
			// Obtain EntityManager reference from JNDI?
			EntityManager em = getPersistenceContext(this.unitName, false);
			if (em == null) {
				// No pre-built EntityManager found -> build one based on factory.
				// Obtain EntityManagerFactory from JNDI?
				EntityManagerFactory emf = getPersistenceUnit(this.unitName);
				if (emf == null) {
					// Need to search for EntityManagerFactory beans.
					emf = findEntityManagerFactory(this.unitName, requestingBeanName);
				}
				// Inject a shared transactional EntityManager proxy.
				if (emf instanceof EntityManagerFactoryInfo &&
						((EntityManagerFactoryInfo) emf).getEntityManagerInterface() != null) {
					// Create EntityManager based on the info's vendor-specific type
					// (which might be more specific than the field's type).
					em = SharedEntityManagerCreator.createSharedEntityManager(emf, this.properties);
				}
				else {
					// Create EntityManager based on the field's type.
					em = SharedEntityManagerCreator.createSharedEntityManager(emf, this.properties, getResourceType());
				}
			}
			return em;
		}

		private EntityManager resolveExtendedEntityManager(Object target, String requestingBeanName) {
			// Obtain EntityManager reference from JNDI?
			EntityManager em = getPersistenceContext(this.unitName, true);
			if (em == null) {
				// No pre-built EntityManager found -> build one based on factory.
				// Obtain EntityManagerFactory from JNDI?
				EntityManagerFactory emf = getPersistenceUnit(this.unitName);
				if (emf == null) {
					// Need to search for EntityManagerFactory beans.
					emf = findEntityManagerFactory(this.unitName, requestingBeanName);
				}
				// Inject a container-managed extended EntityManager.
				em = ExtendedEntityManagerCreator.createContainerManagedEntityManager(emf, this.properties);
			}
			if (em instanceof EntityManagerProxy &&
					beanFactory != null && !beanFactory.isPrototype(requestingBeanName)) {
				extendedEntityManagersToClose.put(target, ((EntityManagerProxy) em).getTargetEntityManager());
			}
			return em;
		}
	}

}
