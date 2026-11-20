/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework.autoproxy.target;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * {@code TargetSourceCreator} that enforces a {@link LazyInitTargetSource} for
 * each bean that is defined as "lazy-init". This will lead to a proxy created for
 * each of those beans, allowing to fetch a reference to such a bean without
 * actually initializing the target bean instance.
 *
 * <p>To be registered as custom {@code TargetSourceCreator} for an auto-proxy
 * creator, in combination with custom interceptors for specific beans or for the
 * creation of lazy-init proxies only. For example, as an autodetected
 * infrastructure bean in an XML application context definition:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator"&gt;
 *   &lt;property name="beanNames" value="*" /&gt; &lt;!-- apply to all beans --&gt;
 *   &lt;property name="customTargetSourceCreators"&gt;
 *     &lt;list&gt;
 *       &lt;bean class="org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator" /&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myLazyInitBean" class="mypackage.MyBeanClass" lazy-init="true"&gt;
 *   &lt;!-- ... --&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.2
 * @see org.springframework.beans.factory.config.BeanDefinition#isLazyInit
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#setCustomTargetSourceCreators
 * @see org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator
 */
public class LazyInitTargetSourceCreator extends AbstractBeanFactoryBasedTargetSourceCreator {

	@Override
	protected boolean isPrototypeBased() {
		return false;
	}

	@Override
	protected @Nullable AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(
			Class<?> beanClass, String beanName) {

		if (getBeanFactory() instanceof ConfigurableListableBeanFactory clbf) {
			BeanDefinition definition = clbf.getBeanDefinition(beanName);
			if (definition.isLazyInit()) {
				return new LazyInitTargetSource();
			}
		}
		return null;
	}

}
