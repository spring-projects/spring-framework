/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that invokes annotated init and destroy methods. Allows for an annotation
 * alternative to Spring's {@link org.springframework.beans.factory.InitializingBean}
 * and {@link org.springframework.beans.factory.DisposableBean} callback interfaces.
 *
 * <p>The actual annotation types that this post-processor checks for can be
 * configured through the {@link #setInitAnnotationType "initAnnotationType"}
 * and {@link #setDestroyAnnotationType "destroyAnnotationType"} properties.
 * Any custom annotation can be used, since there are no required annotation
 * attributes.
 *
 * <p>Init and destroy annotations may be applied to methods of any visibility:
 * public, package-protected, protected, or private. Multiple such methods
 * may be annotated, but it is recommended to only annotate one single
 * init method and destroy method, respectively.
 *
 * <p>Spring's {@link org.springframework.context.annotation.CommonAnnotationBeanPostProcessor}
 * supports the {@link jakarta.annotation.PostConstruct} and {@link jakarta.annotation.PreDestroy}
 * annotations out of the box, as init annotation and destroy annotation, respectively.
 * Furthermore, it also supports the {@link jakarta.annotation.Resource} annotation
 * for annotation-driven injection of named beans.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 2.5
 * @see #setInitAnnotationType
 * @see #setDestroyAnnotationType
 */
@SuppressWarnings("serial")
public class InitDestroyAnnotationBeanPostProcessor implements DestructionAwareBeanPostProcessor,
		MergedBeanDefinitionPostProcessor, BeanRegistrationAotProcessor, PriorityOrdered, Serializable {

	private final transient LifecycleMetadata emptyLifecycleMetadata =
			new LifecycleMetadata(Object.class, Collections.emptyList(), Collections.emptyList()) {
				@Override
				public void checkInitDestroyMethods(RootBeanDefinition beanDefinition) {
				}
				@Override
				public void invokeInitMethods(Object target, String beanName) {
				}
				@Override
				public void invokeDestroyMethods(Object target, String beanName) {
				}
				@Override
				public boolean hasDestroyMethods() {
					return false;
				}
			};


	protected transient Log logger = LogFactory.getLog(getClass());

	private final Set<Class<? extends Annotation>> initAnnotationTypes = new LinkedHashSet<>(2);

	private final Set<Class<? extends Annotation>> destroyAnnotationTypes = new LinkedHashSet<>(2);

	private int order = Ordered.LOWEST_PRECEDENCE;

	@Nullable
	private final transient Map<Class<?>, LifecycleMetadata> lifecycleMetadataCache = new ConcurrentHashMap<>(256);


	/**
	 * Specify the init annotation to check for, indicating initialization
	 * methods to call after configuration of a bean.
	 * <p>Any custom annotation can be used, since there are no required
	 * annotation attributes. There is no default, although a typical choice
	 * is the {@link jakarta.annotation.PostConstruct} annotation.
	 * @see #addInitAnnotationType
	 */
	public void setInitAnnotationType(Class<? extends Annotation> initAnnotationType) {
		this.initAnnotationTypes.clear();
		this.initAnnotationTypes.add(initAnnotationType);
	}

	/**
	 * Add an init annotation to check for, indicating initialization
	 * methods to call after configuration of a bean.
	 * @since 6.0.11
	 * @see #setInitAnnotationType
	 */
	public void addInitAnnotationType(@Nullable Class<? extends Annotation> initAnnotationType) {
		if (initAnnotationType != null) {
			this.initAnnotationTypes.add(initAnnotationType);
		}
	}

	/**
	 * Specify the destroy annotation to check for, indicating destruction
	 * methods to call when the context is shutting down.
	 * <p>Any custom annotation can be used, since there are no required
	 * annotation attributes. There is no default, although a typical choice
	 * is the {@link jakarta.annotation.PreDestroy} annotation.
	 * @see #addDestroyAnnotationType
	 */
	public void setDestroyAnnotationType(Class<? extends Annotation> destroyAnnotationType) {
		this.destroyAnnotationTypes.clear();
		this.destroyAnnotationTypes.add(destroyAnnotationType);
	}

	/**
	 * Add a destroy annotation to check for, indicating destruction
	 * methods to call when the context is shutting down.
	 * @since 6.0.11
	 * @see #setDestroyAnnotationType
	 */
	public void addDestroyAnnotationType(@Nullable Class<? extends Annotation> destroyAnnotationType) {
		if (destroyAnnotationType != null) {
			this.destroyAnnotationTypes.add(destroyAnnotationType);
		}
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanClass, String beanName) {
		findLifecycleMetadata(beanDefinition, beanClass);
	}

	@Override
	@Nullable
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();
		beanDefinition.resolveDestroyMethodIfNecessary();
		LifecycleMetadata metadata = findLifecycleMetadata(beanDefinition, registeredBean.getBeanClass());
		if (!CollectionUtils.isEmpty(metadata.initMethods)) {
			String[] initMethodNames = safeMerge(beanDefinition.getInitMethodNames(), metadata.initMethods);
			beanDefinition.setInitMethodNames(initMethodNames);
		}
		if (!CollectionUtils.isEmpty(metadata.destroyMethods)) {
			String[] destroyMethodNames = safeMerge(beanDefinition.getDestroyMethodNames(), metadata.destroyMethods);
			beanDefinition.setDestroyMethodNames(destroyMethodNames);
		}
		return null;
	}

	private LifecycleMetadata findLifecycleMetadata(RootBeanDefinition beanDefinition, Class<?> beanClass) {
		LifecycleMetadata metadata = findLifecycleMetadata(beanClass);
		metadata.checkInitDestroyMethods(beanDefinition);
		return metadata;
	}

	private static String[] safeMerge(@Nullable String[] existingNames, Collection<LifecycleMethod> detectedMethods) {
		Stream<String> detectedNames = detectedMethods.stream().map(LifecycleMethod::getIdentifier);
		Stream<String> mergedNames = (existingNames != null ?
				Stream.concat(detectedNames, Stream.of(existingNames)) : detectedNames);
		return mergedNames.distinct().toArray(String[]::new);
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
		try {
			metadata.invokeInitMethods(bean, beanName);
		}
		catch (InvocationTargetException ex) {
			throw new BeanCreationException(beanName, "Invocation of init method failed", ex.getTargetException());
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Failed to invoke init method", ex);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
		try {
			metadata.invokeDestroyMethods(bean, beanName);
		}
		catch (InvocationTargetException ex) {
			String msg = "Destroy method on bean with name '" + beanName + "' threw an exception";
			if (logger.isDebugEnabled()) {
				logger.warn(msg, ex.getTargetException());
			}
			else if (logger.isWarnEnabled()) {
				logger.warn(msg + ": " + ex.getTargetException());
			}
		}
		catch (Throwable ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to invoke destroy method on bean with name '" + beanName + "'", ex);
			}
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return findLifecycleMetadata(bean.getClass()).hasDestroyMethods();
	}


	private LifecycleMetadata findLifecycleMetadata(Class<?> beanClass) {
		if (this.lifecycleMetadataCache == null) {
			// Happens after deserialization, during destruction...
			return buildLifecycleMetadata(beanClass);
		}
		// Quick check on the concurrent map first, with minimal locking.
		LifecycleMetadata metadata = this.lifecycleMetadataCache.get(beanClass);
		if (metadata == null) {
			synchronized (this.lifecycleMetadataCache) {
				metadata = this.lifecycleMetadataCache.get(beanClass);
				if (metadata == null) {
					metadata = buildLifecycleMetadata(beanClass);
					this.lifecycleMetadataCache.put(beanClass, metadata);
				}
				return metadata;
			}
		}
		return metadata;
	}

	private LifecycleMetadata buildLifecycleMetadata(final Class<?> beanClass) {
		if (!AnnotationUtils.isCandidateClass(beanClass, this.initAnnotationTypes) &&
				!AnnotationUtils.isCandidateClass(beanClass, this.destroyAnnotationTypes)) {
			return this.emptyLifecycleMetadata;
		}

		List<LifecycleMethod> initMethods = new ArrayList<>();
		List<LifecycleMethod> destroyMethods = new ArrayList<>();
		Class<?> currentClass = beanClass;

		do {
			final List<LifecycleMethod> currInitMethods = new ArrayList<>();
			final List<LifecycleMethod> currDestroyMethods = new ArrayList<>();

			ReflectionUtils.doWithLocalMethods(currentClass, method -> {
				for (Class<? extends Annotation> initAnnotationType : this.initAnnotationTypes) {
					if (initAnnotationType != null && method.isAnnotationPresent(initAnnotationType)) {
						currInitMethods.add(new LifecycleMethod(method, beanClass));
						if (logger.isTraceEnabled()) {
							logger.trace("Found init method on class [" + beanClass.getName() + "]: " + method);
						}
					}
				}
				for (Class<? extends Annotation> destroyAnnotationType : this.destroyAnnotationTypes) {
					if (destroyAnnotationType != null && method.isAnnotationPresent(destroyAnnotationType)) {
						currDestroyMethods.add(new LifecycleMethod(method, beanClass));
						if (logger.isTraceEnabled()) {
							logger.trace("Found destroy method on class [" + beanClass.getName() + "]: " + method);
						}
					}
				}
			});

			initMethods.addAll(0, currInitMethods);
			destroyMethods.addAll(currDestroyMethods);
			currentClass = currentClass.getSuperclass();
		}
		while (currentClass != null && currentClass != Object.class);

		return (initMethods.isEmpty() && destroyMethods.isEmpty() ? this.emptyLifecycleMetadata :
				new LifecycleMetadata(beanClass, initMethods, destroyMethods));
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Initialize transient fields.
		this.logger = LogFactory.getLog(getClass());
	}


	/**
	 * Class representing information about annotated init and destroy methods.
	 */
	private class LifecycleMetadata {

		private final Class<?> beanClass;

		private final Collection<LifecycleMethod> initMethods;

		private final Collection<LifecycleMethod> destroyMethods;

		@Nullable
		private volatile Set<LifecycleMethod> checkedInitMethods;

		@Nullable
		private volatile Set<LifecycleMethod> checkedDestroyMethods;

		public LifecycleMetadata(Class<?> beanClass, Collection<LifecycleMethod> initMethods,
				Collection<LifecycleMethod> destroyMethods) {

			this.beanClass = beanClass;
			this.initMethods = initMethods;
			this.destroyMethods = destroyMethods;
		}

		public void checkInitDestroyMethods(RootBeanDefinition beanDefinition) {
			Set<LifecycleMethod> checkedInitMethods = CollectionUtils.newLinkedHashSet(this.initMethods.size());
			for (LifecycleMethod lifecycleMethod : this.initMethods) {
				String methodIdentifier = lifecycleMethod.getIdentifier();
				if (!beanDefinition.isExternallyManagedInitMethod(methodIdentifier)) {
					beanDefinition.registerExternallyManagedInitMethod(methodIdentifier);
					checkedInitMethods.add(lifecycleMethod);
					if (logger.isTraceEnabled()) {
						logger.trace("Registered init method on class [" + this.beanClass.getName() + "]: " + methodIdentifier);
					}
				}
			}
			Set<LifecycleMethod> checkedDestroyMethods = CollectionUtils.newLinkedHashSet(this.destroyMethods.size());
			for (LifecycleMethod lifecycleMethod : this.destroyMethods) {
				String methodIdentifier = lifecycleMethod.getIdentifier();
				if (!beanDefinition.isExternallyManagedDestroyMethod(methodIdentifier)) {
					beanDefinition.registerExternallyManagedDestroyMethod(methodIdentifier);
					checkedDestroyMethods.add(lifecycleMethod);
					if (logger.isTraceEnabled()) {
						logger.trace("Registered destroy method on class [" + this.beanClass.getName() + "]: " + methodIdentifier);
					}
				}
			}
			this.checkedInitMethods = checkedInitMethods;
			this.checkedDestroyMethods = checkedDestroyMethods;
		}

		public void invokeInitMethods(Object target, String beanName) throws Throwable {
			Collection<LifecycleMethod> checkedInitMethods = this.checkedInitMethods;
			Collection<LifecycleMethod> initMethodsToIterate =
					(checkedInitMethods != null ? checkedInitMethods : this.initMethods);
			if (!initMethodsToIterate.isEmpty()) {
				for (LifecycleMethod lifecycleMethod : initMethodsToIterate) {
					if (logger.isTraceEnabled()) {
						logger.trace("Invoking init method on bean '" + beanName + "': " + lifecycleMethod.getMethod());
					}
					lifecycleMethod.invoke(target);
				}
			}
		}

		public void invokeDestroyMethods(Object target, String beanName) throws Throwable {
			Collection<LifecycleMethod> checkedDestroyMethods = this.checkedDestroyMethods;
			Collection<LifecycleMethod> destroyMethodsToUse =
					(checkedDestroyMethods != null ? checkedDestroyMethods : this.destroyMethods);
			if (!destroyMethodsToUse.isEmpty()) {
				for (LifecycleMethod lifecycleMethod : destroyMethodsToUse) {
					if (logger.isTraceEnabled()) {
						logger.trace("Invoking destroy method on bean '" + beanName + "': " + lifecycleMethod.getMethod());
					}
					lifecycleMethod.invoke(target);
				}
			}
		}

		public boolean hasDestroyMethods() {
			Collection<LifecycleMethod> checkedDestroyMethods = this.checkedDestroyMethods;
			Collection<LifecycleMethod> destroyMethodsToUse =
					(checkedDestroyMethods != null ? checkedDestroyMethods : this.destroyMethods);
			return !destroyMethodsToUse.isEmpty();
		}
	}


	/**
	 * Class representing an annotated init or destroy method.
	 */
	private static class LifecycleMethod {

		private final Method method;

		private final String identifier;

		public LifecycleMethod(Method method, Class<?> beanClass) {
			if (method.getParameterCount() != 0) {
				throw new IllegalStateException("Lifecycle annotation requires a no-arg method: " + method);
			}
			this.method = method;
			this.identifier = (isPrivateOrNotVisible(method, beanClass) ?
					ClassUtils.getQualifiedMethodName(method) : method.getName());
		}

		public Method getMethod() {
			return this.method;
		}

		public String getIdentifier() {
			return this.identifier;
		}

		public void invoke(Object target) throws Throwable {
			ReflectionUtils.makeAccessible(this.method);
			this.method.invoke(target);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof LifecycleMethod that &&
					this.identifier.equals(that.identifier)));
		}

		@Override
		public int hashCode() {
			return this.identifier.hashCode();
		}

		/**
		 * Determine if the supplied lifecycle {@link Method} is private or not
		 * visible to the supplied bean {@link Class}.
		 * @since 6.0.11
		 */
		private static boolean isPrivateOrNotVisible(Method method, Class<?> beanClass) {
			int modifiers = method.getModifiers();
			if (Modifier.isPrivate(modifiers)) {
				return true;
			}
			// Method is declared in a class that resides in a different package
			// than the bean class and the method is neither public nor protected?
			return (!method.getDeclaringClass().getPackageName().equals(beanClass.getPackageName()) &&
					!(Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)));
		}

	}

}
