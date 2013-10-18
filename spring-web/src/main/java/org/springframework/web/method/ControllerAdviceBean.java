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
package org.springframework.web.method;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Encapsulates information about an {@linkplain ControllerAdvice @ControllerAdvice}
 * Spring-managed bean without necessarily requiring it to be instantiated.
 *
 * <p>The {@link #findAnnotatedBeans(ApplicationContext)} method can be used to discover
 * such beans. However, an {@code ControllerAdviceBean} may be created from
 * any object, including ones without an {@code @ControllerAdvice}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.2
 */
public class ControllerAdviceBean implements Ordered {

	private static final Log logger = LogFactory.getLog(ControllerAdviceBean.class);

	private final Object bean;

	private final int order;

	private final BeanFactory beanFactory;

	private final List<Package> basePackages = new ArrayList<Package>();

	private final List<Class<? extends Annotation>> annotations = new ArrayList<Class<? extends Annotation>>();

	private final List<Class<?>> assignableTypes = new ArrayList<Class<?>>();


	/**
	 * Create an instance using the given bean name.
	 * @param beanName the name of the bean
	 * @param beanFactory a BeanFactory that can be used later to resolve the bean
	 */
	public ControllerAdviceBean(String beanName, BeanFactory beanFactory) {
		Assert.hasText(beanName, "'beanName' must not be null");
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		Assert.isTrue(beanFactory.containsBean(beanName),
				"Bean factory [" + beanFactory + "] does not contain bean " + "with name [" + beanName + "]");

		this.bean = beanName;
		this.beanFactory = beanFactory;

		Class<?> beanType = this.beanFactory.getType(beanName);
		this.order = initOrderFromBeanType(beanType);

		ControllerAdvice annotation = AnnotationUtils.findAnnotation(beanType,ControllerAdvice.class);
		Assert.notNull(annotation, "BeanType [" + beanType.getName() + "] is not annotated @ControllerAdvice");

		this.basePackages.addAll(initBasePackagesFromBeanType(beanType, annotation));
		this.annotations.addAll(Arrays.asList(annotation.annotations()));
		this.assignableTypes.addAll(Arrays.asList(annotation.assignableTypes()));
	}

	private static int initOrderFromBeanType(Class<?> beanType) {
		Order annot = AnnotationUtils.findAnnotation(beanType, Order.class);
		return (annot != null) ? annot.value() : Ordered.LOWEST_PRECEDENCE;
	}

	private static List<Package> initBasePackagesFromBeanType(Class<?> beanType, ControllerAdvice annotation) {
		List<Package> basePackages = new ArrayList<Package>();
		List<String> basePackageNames = new ArrayList<String>();
		basePackageNames.addAll(Arrays.asList(annotation.value()));
		basePackageNames.addAll(Arrays.asList(annotation.basePackages()));
		for (String pkgName : basePackageNames) {
			if (StringUtils.hasText(pkgName)) {
				Package pkg = Package.getPackage(pkgName);
				if(pkg != null) {
					basePackages.add(pkg);
				}
				else {
					logger.warn("Package [" + pkgName + "] was not found, see [" + beanType.getName() + "]");
				}
			}
		}
		for (Class<?> markerClass : annotation.basePackageClasses()) {
			Package pack = markerClass.getPackage();
			if (pack != null) {
				basePackages.add(pack);
			}
			else {
				logger.warn("Package was not found for class [" + markerClass.getName()
						+ "], see [" + beanType.getName() + "]");
			}
		}
		return basePackages;
	}

	/**
	 * Create an instance using the given bean instance.
	 * @param bean the bean
	 */
	public ControllerAdviceBean(Object bean) {
		Assert.notNull(bean, "'bean' must not be null");
		this.bean = bean;
		this.order = initOrderFromBean(bean);

		Class<? extends Object> beanType = bean.getClass();
		ControllerAdvice annotation = AnnotationUtils.findAnnotation(beanType,ControllerAdvice.class);
		Assert.notNull(annotation, "BeanType [" + beanType.getName() + "] is not annotated @ControllerAdvice");

		this.basePackages.addAll(initBasePackagesFromBeanType(beanType, annotation));
		this.annotations.addAll(Arrays.asList(annotation.annotations()));
		this.assignableTypes.addAll(Arrays.asList(annotation.assignableTypes()));
		this.beanFactory = null;
	}

	private static int initOrderFromBean(Object bean) {
		return (bean instanceof Ordered) ? ((Ordered) bean).getOrder() : initOrderFromBeanType(bean.getClass());
	}

	/**
	 * Find the names of beans annotated with
	 * {@linkplain ControllerAdvice @ControllerAdvice} in the given
	 * ApplicationContext and wrap them as {@code ControllerAdviceBean} instances.
	 */
	public static List<ControllerAdviceBean> findAnnotatedBeans(ApplicationContext applicationContext) {
		List<ControllerAdviceBean> beans = new ArrayList<ControllerAdviceBean>();
		for (String name : applicationContext.getBeanDefinitionNames()) {
			if (applicationContext.findAnnotationOnBean(name, ControllerAdvice.class) != null) {
				beans.add(new ControllerAdviceBean(name, applicationContext));
			}
		}
		return beans;
	}

	/**
	 * Returns the order value extracted from the {@link ControllerAdvice}
	 * annotation or {@link Ordered#LOWEST_PRECEDENCE} otherwise.
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Returns the type of the contained bean.
	 * If the bean type is a CGLIB-generated class, the original, user-defined class is returned.
	 */
	public Class<?> getBeanType() {
		Class<?> clazz = (this.bean instanceof String)
				? this.beanFactory.getType((String) this.bean) : this.bean.getClass();

		return ClassUtils.getUserClass(clazz);
	}

	/**
	 * Return a bean instance if necessary resolving the bean name through the BeanFactory.
	 */
	public Object resolveBean() {
		return (this.bean instanceof String) ? this.beanFactory.getBean((String) this.bean) : this.bean;
	}

	/**
	 * Checks whether the given bean type should be assisted by this
	 * {@code @ControllerAdvice} instance.
	 *
	 * @param beanType the type of the bean to check
	 * @see org.springframework.web.bind.annotation.ControllerAdvice
	 * @since 4.0
	 */
	public boolean isApplicableToBeanType(Class<?> beanType) {
		if(!hasSelectors()) {
			return true;
		}
		else if (beanType != null) {
			for (Class<?> clazz : this.assignableTypes) {
				if(ClassUtils.isAssignable(clazz, beanType)) {
					return true;
				}
			}
			for (Class<? extends Annotation> annotationClass : this.annotations) {
				if(AnnotationUtils.findAnnotation(beanType, annotationClass) != null) {
					return true;
				}
			}
			String packageName = beanType.getPackage().getName();
			for (Package basePackage : this.basePackages) {
				if(packageName.startsWith(basePackage.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasSelectors() {
		return (!this.basePackages.isEmpty() || !this.annotations.isEmpty() || !this.assignableTypes.isEmpty());
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o != null && o instanceof ControllerAdviceBean) {
			ControllerAdviceBean other = (ControllerAdviceBean) o;
			return this.bean.equals(other.bean);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 31 * this.bean.hashCode();
	}

	@Override
	public String toString() {
		return bean.toString();
	}

}
