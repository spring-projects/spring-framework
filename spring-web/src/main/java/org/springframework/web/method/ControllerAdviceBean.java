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
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Encapsulates information about an {@link ControllerAdvice @ControllerAdvice}
 * Spring-managed bean without necessarily requiring it to be instantiated.
 *
 * <p>The {@link #findAnnotatedBeans(ApplicationContext)} method can be used to
 * discover such beans. However, a {@code ControllerAdviceBean} may be created
 * from any object, including ones without an {@code @ControllerAdvice} annotation.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.2
 */
public class ControllerAdviceBean implements Ordered {

	/**
	 * Reference to the actual bean instance or a {@code String} representing
	 * the bean name.
	 */
	private final Object beanOrName;

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

	@Nullable
	private final BeanFactory beanFactory;

	@Nullable
	private Integer order;


	/**
	 * Create a {@code ControllerAdviceBean} using the given bean instance.
	 * @param bean the bean instance
	 */
	public ControllerAdviceBean(Object bean) {
		Assert.notNull(bean, "Bean must not be null");
		this.beanOrName = bean;
		this.isSingleton = true;
		this.resolvedBean = bean;
		this.beanType = ClassUtils.getUserClass(bean.getClass());
		this.beanTypePredicate = createBeanTypePredicate(this.beanType);
		this.beanFactory = null;
	}

	/**
	 * Create a {@code ControllerAdviceBean} using the given bean name and
	 * {@code BeanFactory}.
	 * @param beanName the name of the bean
	 * @param beanFactory a {@code BeanFactory} to retrieve the bean type initially
	 * and later to resolve the actual bean
	 */
	public ControllerAdviceBean(String beanName, BeanFactory beanFactory) {
		this(beanName, beanFactory, null);
	}

	/**
	 * Create a {@code ControllerAdviceBean} using the given bean name,
	 * {@code BeanFactory}, and {@link ControllerAdvice @ControllerAdvice}
	 * annotation.
	 * @param beanName the name of the bean
	 * @param beanFactory a {@code BeanFactory} to retrieve the bean type initially
	 * and later to resolve the actual bean
	 * @param controllerAdvice the {@code @ControllerAdvice} annotation for the
	 * bean, or {@code null} if not yet retrieved
	 * @since 5.2
	 */
	public ControllerAdviceBean(String beanName, BeanFactory beanFactory, @Nullable ControllerAdvice controllerAdvice) {
		Assert.hasText(beanName, "Bean name must contain text");
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.isTrue(beanFactory.containsBean(beanName), () -> "BeanFactory [" + beanFactory +
				"] does not contain specified controller advice bean '" + beanName + "'");

		this.beanOrName = beanName;
		this.isSingleton = beanFactory.isSingleton(beanName);
		this.beanType = getBeanType(beanName, beanFactory);
		this.beanTypePredicate = (controllerAdvice != null ? createBeanTypePredicate(controllerAdvice) :
				createBeanTypePredicate(this.beanType));
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
			String beanName = null;
			Object resolvedBean = null;
			if (this.beanFactory != null && this.beanOrName instanceof String stringBeanName) {
				beanName = stringBeanName;
				String targetBeanName = ScopedProxyUtils.getTargetBeanName(beanName);
				boolean isScopedProxy = this.beanFactory.containsBean(targetBeanName);
				// Avoid eager @ControllerAdvice bean resolution for scoped proxies,
				// since attempting to do so during context initialization would result
				// in an exception due to the current absence of the scope. For example,
				// an HTTP request or session scope is not active during initialization.
				if (!isScopedProxy && !ScopedProxyUtils.isScopedTarget(beanName)) {
					resolvedBean = resolveBean();
				}
			}
			else {
				resolvedBean = resolveBean();
			}

			if (resolvedBean instanceof Ordered ordered) {
				this.order = ordered.getOrder();
			}
			else {
				if (beanName != null && this.beanFactory instanceof ConfigurableBeanFactory cbf) {
					try {
						BeanDefinition bd = cbf.getMergedBeanDefinition(beanName);
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
	 * <p>As of Spring Framework 5.2, once the bean instance has been resolved it
	 * will be cached if it is a singleton, thereby avoiding repeated lookups in
	 * the {@code BeanFactory}.
	 */
	public Object resolveBean() {
		if (this.resolvedBean == null) {
			// this.beanOrName must be a String representing the bean name if
			// this.resolvedBean is null.
			Object resolvedBean = obtainBeanFactory().getBean((String) this.beanOrName);
			// Don't cache non-singletons (e.g., prototypes).
			if (!this.isSingleton) {
				return resolvedBean;
			}
			this.resolvedBean = resolvedBean;
		}
		return this.resolvedBean;
	}

	private BeanFactory obtainBeanFactory() {
		Assert.state(this.beanFactory != null, "No BeanFactory set");
		return this.beanFactory;
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
				this.beanOrName.equals(that.beanOrName) && this.beanFactory == that.beanFactory));
	}

	@Override
	public int hashCode() {
		return this.beanOrName.hashCode();
	}

	@Override
	public String toString() {
		return this.beanOrName.toString();
	}


	/**
	 * Find beans annotated with {@link ControllerAdvice @ControllerAdvice} in the
	 * given {@link ApplicationContext} and wrap them as {@code ControllerAdviceBean}
	 * instances.
	 * <p>As of Spring Framework 5.2, the {@code ControllerAdviceBean} instances
	 * in the returned list are sorted using {@link OrderComparator#sort(List)}.
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

	private static HandlerTypePredicate createBeanTypePredicate(@Nullable Class<?> beanType) {
		ControllerAdvice controllerAdvice = (beanType != null ?
				AnnotatedElementUtils.findMergedAnnotation(beanType, ControllerAdvice.class) : null);
		return createBeanTypePredicate(controllerAdvice);
	}

	private static HandlerTypePredicate createBeanTypePredicate(@Nullable ControllerAdvice controllerAdvice) {
		if (controllerAdvice != null) {
			return HandlerTypePredicate.builder()
					.basePackage(controllerAdvice.basePackages())
					.basePackageClass(controllerAdvice.basePackageClasses())
					.assignableType(controllerAdvice.assignableTypes())
					.annotation(controllerAdvice.annotations())
					.build();
		}
		return HandlerTypePredicate.forAnyHandlerType();
	}

}
