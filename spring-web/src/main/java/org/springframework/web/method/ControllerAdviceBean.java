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

package org.springframework.web.method;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Encapsulates information about an {@link ControllerAdvice @ControllerAdvice}
 * Spring-managed bean without necessarily requiring it to be instantiated.
 * The {@link #findAnnotatedBeans(ApplicationContext)} method can be used to
 * discover such beans.
 *
 * <p>This class is internal to Spring Framework and is not meant to be used
 * by applications to manually create {@code @ControllerAdvice} beans.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.2
 */
public class ControllerAdviceBean implements Ordered {

	private final String beanName;

	private final boolean isSingleton;

	/**
	 * Reference to the resolved bean instance, potentially lazily retrieved
	 * via the {@code BeanFactory}.
	 */
	@Nullable
	private Object resolvedBean;

	@Nullable
	private final Class<?> beanType;

	private final HandlerTypePredicate beanTypePredicate;

	private final BeanFactory beanFactory;

	@Nullable
	private Integer order;


	/**
	 * Create a {@code ControllerAdviceBean} using the given bean name,
	 * {@code BeanFactory}, and {@link ControllerAdvice @ControllerAdvice}
	 * annotation.
	 * @param beanName the name of the bean
	 * @param beanFactory a {@code BeanFactory} to retrieve the bean type initially
	 * and later to resolve the actual bean
	 * @param controllerAdvice the {@code @ControllerAdvice} annotation for the bean
	 * @since 5.2
	 */
	public ControllerAdviceBean(String beanName, BeanFactory beanFactory, ControllerAdvice controllerAdvice) {
		Assert.hasText(beanName, "Bean name must contain text");
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.isTrue(beanFactory.containsBean(beanName), () -> "BeanFactory [" + beanFactory +
				"] does not contain specified controller advice bean '" + beanName + "'");
		Assert.notNull(controllerAdvice, "ControllerAdvice must not be null");

		this.beanName = beanName;
		this.isSingleton = beanFactory.isSingleton(beanName);
		this.beanType = getBeanType(beanName, beanFactory);
		this.beanTypePredicate = createBeanTypePredicate(controllerAdvice);
		this.beanFactory = beanFactory;
	}


	/**
	 * Get the order value for the contained bean.
	 * <p>As of Spring Framework 5.3, the order value is lazily retrieved using
	 * the following algorithm and cached. Note, however, that a
	 * {@link ControllerAdvice @ControllerAdvice} bean that is configured as a
	 * scoped bean &mdash; for example, as a request-scoped or session-scoped
	 * bean &mdash; will not be eagerly resolved. Consequently, {@link Ordered} is
	 * not honored for scoped {@code @ControllerAdvice} beans.
	 * <ul>
	 * <li>If the {@linkplain #resolveBean resolved bean} implements {@link Ordered},
	 * use the value returned by {@link Ordered#getOrder()}.</li>
	 * <li>If the {@linkplain org.springframework.context.annotation.Bean factory method}
	 * is known, use the value returned by {@link OrderUtils#getOrder(AnnotatedElement)}.
	 * <li>If the {@linkplain #getBeanType() bean type} is known, use the value returned
	 * by {@link OrderUtils#getOrder(Class, int)} with {@link Ordered#LOWEST_PRECEDENCE}
	 * used as the default order value.</li>
	 * <li>Otherwise use {@link Ordered#LOWEST_PRECEDENCE} as the default, fallback
	 * order value.</li>
	 * </ul>
	 * @see #resolveBean()
	 */
	@Override
	public int getOrder() {
		if (this.order == null) {
			Object resolvedBean = null;
			String targetBeanName = ScopedProxyUtils.getTargetBeanName(this.beanName);
			boolean isScopedProxy = this.beanFactory.containsBean(targetBeanName);
			// Avoid eager @ControllerAdvice bean resolution for scoped proxies,
			// since attempting to do so during context initialization would result
			// in an exception due to the current absence of the scope. For example,
			// an HTTP request or session scope is not active during initialization.
			if (!isScopedProxy && !ScopedProxyUtils.isScopedTarget(this.beanName)) {
				resolvedBean = resolveBean();
			}

			if (resolvedBean instanceof Ordered ordered) {
				this.order = ordered.getOrder();
			}
			else {
				if (this.beanFactory instanceof ConfigurableBeanFactory cbf) {
					try {
						BeanDefinition bd = cbf.getMergedBeanDefinition(this.beanName);
						if (bd instanceof RootBeanDefinition rbd) {
							Method factoryMethod = rbd.getResolvedFactoryMethod();
							if (factoryMethod != null) {
								this.order = OrderUtils.getOrder(factoryMethod);
							}
						}
					}
					catch (NoSuchBeanDefinitionException ex) {
						// ignore -> probably a manually registered singleton
					}
				}
				if (this.order == null) {
					if (this.beanType != null) {
						this.order = OrderUtils.getOrder(this.beanType, Ordered.LOWEST_PRECEDENCE);
					}
					else {
						this.order = Ordered.LOWEST_PRECEDENCE;
					}
				}
			}
		}
		return this.order;
	}

	/**
	 * Return the type of the contained bean.
	 * <p>If the bean type is a CGLIB-generated class, the original user-defined
	 * class is returned.
	 */
	@Nullable
	public Class<?> getBeanType() {
		return this.beanType;
	}

	/**
	 * Get the bean instance for this {@code ControllerAdviceBean}, if necessary
	 * resolving the bean name through the {@link BeanFactory}.
	 * <p>Once the bean instance has been resolved it will be cached if it is a
	 * singleton, thereby avoiding repeated lookups in the {@code BeanFactory}.
	 */
	public Object resolveBean() {
		if (this.resolvedBean == null) {
			Object resolvedBean = this.beanFactory.getBean(this.beanName);
			// Don't cache non-singletons (e.g., prototypes).
			if (!this.isSingleton) {
				return resolvedBean;
			}
			this.resolvedBean = resolvedBean;
		}
		return this.resolvedBean;
	}

	/**
	 * Check whether the given bean type should be advised by this
	 * {@code ControllerAdviceBean}.
	 * @param beanType the type of the bean to check
	 * @since 4.0
	 * @see ControllerAdvice
	 */
	public boolean isApplicableToBeanType(@Nullable Class<?> beanType) {
		return this.beanTypePredicate.test(beanType);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ControllerAdviceBean that &&
				this.beanName.equals(that.beanName) && this.beanFactory == that.beanFactory));
	}

	@Override
	public int hashCode() {
		return this.beanName.hashCode();
	}

	@Override
	public String toString() {
		return this.beanName;
	}


	/**
	 * Find beans annotated with {@link ControllerAdvice @ControllerAdvice} in the
	 * given {@link ApplicationContext} and wrap them as {@code ControllerAdviceBean}
	 * instances.
	 * <p>Note that the {@code ControllerAdviceBean} instances in the returned list
	 * are sorted using {@link OrderComparator#sort(List)}.
	 * @see #getOrder()
	 * @see OrderComparator
	 * @see Ordered
	 */
	public static List<ControllerAdviceBean> findAnnotatedBeans(ApplicationContext context) {
		ListableBeanFactory beanFactory = context;
		if (context instanceof ConfigurableApplicationContext cac) {
			// Use internal BeanFactory for potential downcast to ConfigurableBeanFactory above
			beanFactory = cac.getBeanFactory();
		}
		List<ControllerAdviceBean> adviceBeans = new ArrayList<>();
		for (String name : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Object.class)) {
			if (!ScopedProxyUtils.isScopedTarget(name)) {
				ControllerAdvice controllerAdvice = beanFactory.findAnnotationOnBean(name, ControllerAdvice.class);
				if (controllerAdvice != null) {
					// Use the @ControllerAdvice annotation found by findAnnotationOnBean()
					// in order to avoid a subsequent lookup of the same annotation.
					adviceBeans.add(new ControllerAdviceBean(name, beanFactory, controllerAdvice));
				}
			}
		}
		OrderComparator.sort(adviceBeans);
		return adviceBeans;
	}

	@Nullable
	private static Class<?> getBeanType(String beanName, BeanFactory beanFactory) {
		Class<?> beanType = beanFactory.getType(beanName);
		return (beanType != null ? ClassUtils.getUserClass(beanType) : null);
	}

	private static HandlerTypePredicate createBeanTypePredicate(ControllerAdvice controllerAdvice) {
		return HandlerTypePredicate.builder()
				.basePackage(controllerAdvice.basePackages())
				.basePackageClass(controllerAdvice.basePackageClasses())
				.assignableType(controllerAdvice.assignableTypes())
				.annotation(controllerAdvice.annotations())
				.build();
	}

}
