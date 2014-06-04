/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.annotation.OrderProvider;

/**
 * An {@link OrderProvider} implementation that is aware of the
 * bean metadata of the instances to sort.
 *
 * <p>Lookup for the method factory of an instance to sort, if
 * any and retrieve the {@link Order} value defined on it. This
 * essentially allows for the following construct:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Bean
 *     &#064;Order(5)
 *     public MyService myService() {
 *         return new MyService();
 *     }
 * }</pre>
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class FactoryAwareOrderProvider implements OrderProvider {

	private final Map<Object, String> instancesToBeanNames;

	private final ConfigurableListableBeanFactory beanFactory;

	public FactoryAwareOrderProvider(Map<Object, String> instancesToBeanNames,
			ConfigurableListableBeanFactory beanFactory) {
		this.instancesToBeanNames = instancesToBeanNames;
		this.beanFactory = beanFactory;
	}

	@Override
	public Integer getOrder(Object obj) {
		Method factoryMethod = getFactoryMethod(instancesToBeanNames.get(obj));
		if (factoryMethod != null) {
			Order order = AnnotationUtils.getAnnotation(factoryMethod, Order.class);
			if (order != null) {
				return order.value();
			}
		}
		return null;
	}

	private Method getFactoryMethod(String beanName) {
		if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
			BeanDefinition bd = beanFactory.getMergedBeanDefinition(beanName);
			if (bd instanceof RootBeanDefinition) {
				return ((RootBeanDefinition) bd).getResolvedFactoryMethod();
			}
		}
		return null;
	}

}
