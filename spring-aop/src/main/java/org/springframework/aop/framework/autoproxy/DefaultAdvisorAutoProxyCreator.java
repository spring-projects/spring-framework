/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework.autoproxy;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanNameAware;

/**
 * {@code BeanPostProcessor} implementation that creates AOP proxies based on all
 * candidate {@code Advisor}s in the current {@code BeanFactory}. This class is
 * completely generic; it contains no special code to handle any particular aspects,
 * such as pooling aspects.
 *
 * <p>It's possible to filter out advisors - for example, to use multiple post processors
 * of this type in the same factory - by setting the {@code usePrefix} property to true,
 * in which case only advisors beginning with the DefaultAdvisorAutoProxyCreator's bean
 * name followed by a dot (like "aapc.") will be used. This default prefix can be changed
 * from the bean name by setting the {@code advisorBeanNamePrefix} property.
 * The separator (.) will also be used in this case.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 */
@SuppressWarnings("serial")
public class DefaultAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator implements BeanNameAware {

	/** Separator between prefix and remainder of bean name. */
	public static final String SEPARATOR = ".";


	private boolean usePrefix = false;

	private @Nullable String advisorBeanNamePrefix;


	/**
	 * Set whether to only include advisors with a certain prefix in the bean name.
	 * <p>Default is {@code false}, including all beans of type {@code Advisor}.
	 * @see #setAdvisorBeanNamePrefix
	 */
	public void setUsePrefix(boolean usePrefix) {
		this.usePrefix = usePrefix;
	}

	/**
	 * Return whether to only include advisors with a certain prefix in the bean name.
	 */
	public boolean isUsePrefix() {
		return this.usePrefix;
	}

	/**
	 * Set the prefix for bean names that will cause them to be included for
	 * auto-proxying by this object. This prefix should be set to avoid circular
	 * references. Default value is the bean name of this object + a dot.
	 * @param advisorBeanNamePrefix the exclusion prefix
	 */
	public void setAdvisorBeanNamePrefix(@Nullable String advisorBeanNamePrefix) {
		this.advisorBeanNamePrefix = advisorBeanNamePrefix;
	}

	/**
	 * Return the prefix for bean names that will cause them to be included
	 * for auto-proxying by this object.
	 */
	public @Nullable String getAdvisorBeanNamePrefix() {
		return this.advisorBeanNamePrefix;
	}

	@Override
	public void setBeanName(String name) {
		// If no infrastructure bean name prefix has been set, override it.
		if (this.advisorBeanNamePrefix == null) {
			this.advisorBeanNamePrefix = name + SEPARATOR;
		}
	}


	/**
	 * Consider {@code Advisor} beans with the specified prefix as eligible, if activated.
	 * @see #setUsePrefix
	 * @see #setAdvisorBeanNamePrefix
	 */
	@Override
	protected boolean isEligibleAdvisorBean(String beanName) {
		if (!isUsePrefix()) {
			return true;
		}
		String prefix = getAdvisorBeanNamePrefix();
		return (prefix != null && beanName.startsWith(prefix));
	}

}
