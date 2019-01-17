/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Encapsulates information about an {@linkplain ControllerAdvice @ControllerAdvice}
 * Spring-managed bean without necessarily requiring it to be instantiated.
 *
 * <p>The {@link #findAnnotatedBeans(ApplicationContext)} method can be used to
 * discover such beans. However, a {@code ControllerAdviceBean} may be created
 * from any object, including ones without an {@code @ControllerAdvice}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 3.2
 */
public class ControllerAdviceBean implements Ordered {

	private final Object bean;

	@Nullable
	private final BeanFactory beanFactory;

	private final int order;

	private final HandlerTypePredicate beanTypePredicate;


	/**
	 * Create a {@code ControllerAdviceBean} using the given bean instance.
	 * @param bean the bean instance
	 */
	public ControllerAdviceBean(Object bean) {
		this(bean, null);
	}

	/**
	 * Create a {@code ControllerAdviceBean} using the given bean name.
	 * @param beanName the name of the bean
	 * @param beanFactory a BeanFactory that can be used later to resolve the bean
	 */
	public ControllerAdviceBean(String beanName, @Nullable BeanFactory beanFactory) {
		this((Object) beanName, beanFactory);
	}

	private ControllerAdviceBean(Object bean, @Nullable BeanFactory beanFactory) {
		this.bean = bean;
		this.beanFactory = beanFactory;
		Class<?> beanType;

		if (bean instanceof String) {
			String beanName = (String) bean;
			Assert.hasText(beanName, "Bean name must not be null");
			Assert.notNull(beanFactory, "BeanFactory must not be null");
			if (!beanFactory.containsBean(beanName)) {
				throw new IllegalArgumentException("BeanFactory [" + beanFactory +
						"] does not contain specified controller advice bean '" + beanName + "'");
			}
			beanType = this.beanFactory.getType(beanName);
			this.order = initOrderFromBeanType(beanType);
		}
		else {
			Assert.notNull(bean, "Bean must not be null");
			beanType = bean.getClass();
			this.order = initOrderFromBean(bean);
		}

		ControllerAdvice annotation = (beanType != null ?
				AnnotatedElementUtils.findMergedAnnotation(beanType, ControllerAdvice.class) : null);

		if (annotation != null) {
			this.beanTypePredicate = HandlerTypePredicate.builder()
					.basePackage(annotation.basePackages())
					.basePackageClass(annotation.basePackageClasses())
					.assignableType(annotation.assignableTypes())
					.annotation(annotation.annotations())
					.build();
		}
		else {
			this.beanTypePredicate = HandlerTypePredicate.forAnyHandlerType();
		}
	}


	/**
	 * Returns the order value extracted from the {@link ControllerAdvice}
	 * annotation, or {@link Ordered#LOWEST_PRECEDENCE} otherwise.
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Return the type of the contained bean.
	 * <p>If the bean type is a CGLIB-generated class, the original
	 * user-defined class is returned.
	 */
	@Nullable
	public Class<?> getBeanType() {
		Class<?> beanType = (this.bean instanceof String ?
				obtainBeanFactory().getType((String) this.bean) : this.bean.getClass());
		return (beanType != null ? ClassUtils.getUserClass(beanType) : null);
	}

	/**
	 * Return a bean instance if necessary resolving the bean name through the BeanFactory.
	 */
	public Object resolveBean() {
		return (this.bean instanceof String ? obtainBeanFactory().getBean((String) this.bean) : this.bean);
	}

	private BeanFactory obtainBeanFactory() {
		Assert.state(this.beanFactory != null, "No BeanFactory set");
		return this.beanFactory;
	}

	/**
	 * Check whether the given bean type should be assisted by this
	 * {@code @ControllerAdvice} instance.
	 * @param beanType the type of the bean to check
	 * @since 4.0
	 * @see org.springframework.web.bind.annotation.ControllerAdvice
	 */
	public boolean isApplicableToBeanType(@Nullable Class<?> beanType) {
		return this.beanTypePredicate.test(beanType);
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ControllerAdviceBean)) {
			return false;
		}
		ControllerAdviceBean otherAdvice = (ControllerAdviceBean) other;
		return (this.bean.equals(otherAdvice.bean) && this.beanFactory == otherAdvice.beanFactory);
	}

	@Override
	public int hashCode() {
		return this.bean.hashCode();
	}

	@Override
	public String toString() {
		return this.bean.toString();
	}


	/**
	 * Find the names of beans annotated with
	 * {@linkplain ControllerAdvice @ControllerAdvice} in the given
	 * ApplicationContext and wrap them as {@code ControllerAdviceBean} instances.
	 */
	public static List<ControllerAdviceBean> findAnnotatedBeans(ApplicationContext context) {
		return Arrays.stream(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context, Object.class))
				.filter(name -> context.findAnnotationOnBean(name, ControllerAdvice.class) != null)
				.map(name -> new ControllerAdviceBean(name, context))
				.collect(Collectors.toList());
	}

	private static int initOrderFromBean(Object bean) {
		return (bean instanceof Ordered ? ((Ordered) bean).getOrder() : initOrderFromBeanType(bean.getClass()));
	}

	private static int initOrderFromBeanType(@Nullable Class<?> beanType) {
		Integer order = null;
		if (beanType != null) {
			order = OrderUtils.getOrder(beanType);
		}
		return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
	}

}
