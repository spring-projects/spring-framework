/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.scripting.support.ScriptFactoryPostProcessor;

/**
 * Utilities for use with {@link LangNamespaceHandler}.
 *
 * @author Rob Harrop
 * @author Mark Fisher
 * @since 2.5
 * @deprecated with no replacement as not actively maintained anymore
 */
@Deprecated(since = "7.0")
public abstract class LangNamespaceUtils {

	/**
	 * The unique name under which the internally managed {@link ScriptFactoryPostProcessor} is
	 * registered in the {@link BeanDefinitionRegistry}.
	 */
	private static final String SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME =
			"org.springframework.scripting.config.scriptFactoryPostProcessor";


	/**
	 * Register a {@link ScriptFactoryPostProcessor} bean definition in the supplied
	 * {@link BeanDefinitionRegistry} if the {@link ScriptFactoryPostProcessor} hasn't
	 * already been registered.
	 * @param registry the {@link BeanDefinitionRegistry} to register the script processor with
	 * @return the {@link ScriptFactoryPostProcessor} bean definition (new or already registered)
	 */
	public static BeanDefinition registerScriptFactoryPostProcessorIfNecessary(BeanDefinitionRegistry registry) {
		BeanDefinition beanDefinition;
		if (registry.containsBeanDefinition(SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME)) {
			beanDefinition = registry.getBeanDefinition(SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME);
		}
		else {
			beanDefinition = new RootBeanDefinition(ScriptFactoryPostProcessor.class);
			registry.registerBeanDefinition(SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME, beanDefinition);
		}
		return beanDefinition;
	}

}
