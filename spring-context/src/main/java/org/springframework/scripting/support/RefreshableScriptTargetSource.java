/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.support;

import org.springframework.aop.target.dynamic.BeanFactoryRefreshableTargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * Subclass of {@link BeanFactoryRefreshableTargetSource} that determines whether
 * a refresh is required through the given {@link ScriptFactory}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.0
 */
public class RefreshableScriptTargetSource extends BeanFactoryRefreshableTargetSource {

	private final ScriptFactory scriptFactory;

	private final ScriptSource scriptSource;

	private final boolean isFactoryBean;


	/**
	 * Create a new RefreshableScriptTargetSource.
	 * @param beanFactory the BeanFactory to fetch the scripted bean from
	 * @param beanName the name of the target bean
	 * @param scriptFactory the ScriptFactory to delegate to for determining
	 * whether a refresh is required
	 * @param scriptSource the ScriptSource for the script definition
	 * @param isFactoryBean whether the target script defines a FactoryBean
	 */
	public RefreshableScriptTargetSource(BeanFactory beanFactory, String beanName,
			ScriptFactory scriptFactory, ScriptSource scriptSource, boolean isFactoryBean) {

		super(beanFactory, beanName);
		Assert.notNull(scriptFactory, "ScriptFactory must not be null");
		Assert.notNull(scriptSource, "ScriptSource must not be null");
		this.scriptFactory = scriptFactory;
		this.scriptSource = scriptSource;
		this.isFactoryBean = isFactoryBean;
	}


	/**
	 * Determine whether a refresh is required through calling
	 * ScriptFactory's {@code requiresScriptedObjectRefresh} method.
	 * @see ScriptFactory#requiresScriptedObjectRefresh(ScriptSource)
	 */
	@Override
	protected boolean requiresRefresh() {
		return this.scriptFactory.requiresScriptedObjectRefresh(this.scriptSource);
	}

	/**
	 * Obtain a fresh target object, retrieving a FactoryBean if necessary.
	 */
	@Override
	protected Object obtainFreshBean(BeanFactory beanFactory, String beanName) {
		return super.obtainFreshBean(beanFactory,
				(this.isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName));
	}

}
