/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * Advisor driven by a {@link JCacheOperationSource}, used to include a
 * cache advice bean for methods that are cacheable.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see #setAdviceBeanName
 * @see JCacheInterceptor
 */
@SuppressWarnings("serial")
public class BeanFactoryJCacheOperationSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

	private final JCacheOperationSourcePointcut pointcut = new JCacheOperationSourcePointcut();


	/**
	 * Set the cache operation attribute source which is used to find cache
	 * attributes. This should usually be identical to the source reference
	 * set on the cache interceptor itself.
	 * @see JCacheInterceptor#setCacheOperationSource
	 */
	public void setCacheOperationSource(JCacheOperationSource cacheOperationSource) {
		this.pointcut.setCacheOperationSource(cacheOperationSource);
	}

	/**
	 * Set the {@link org.springframework.aop.ClassFilter} to use for this pointcut.
	 * Default is {@link org.springframework.aop.ClassFilter#TRUE}.
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.pointcut.setClassFilter(classFilter);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
