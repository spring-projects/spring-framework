package org.springframework.config.java.support;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.config.java.ConfigurationModel;
import org.springframework.config.java.internal.parsing.ConfigurationParser;

public abstract class AbstractConfigurationClassProcessor {
	

	protected abstract BeanDefinitionRegistry getConfigurationBeanDefinitions(boolean includeAbstractBeanDefs);
	
	protected abstract ConfigurationParser createConfigurationParser();
	
	protected abstract void validateModel(ConfigurationModel configModel);
	
	protected BeanDefinitionRegistry processConfigBeanDefinitions() {
		BeanDefinitionRegistry configBeanDefs = getConfigurationBeanDefinitions(false);
		
		if(configBeanDefs.getBeanDefinitionCount() == 0)
			return configBeanDefs; // nothing to do - don't waste any more cycles
		
		ConfigurationModel configModel = createConfigurationModelFor(configBeanDefs);
		
		validateModel(configModel);
		
		return renderModelAsBeanDefinitions(configModel);
	}
	
	private ConfigurationModel createConfigurationModelFor(BeanDefinitionRegistry configBeanDefinitions) {

		ConfigurationParser parser = createConfigurationParser();
		
		for(String beanName : configBeanDefinitions.getBeanDefinitionNames()) {
			BeanDefinition beanDef = configBeanDefinitions.getBeanDefinition(beanName);
			String className = beanDef.getBeanClassName();
			
			parser.parse(className, beanName);
		}
		
	    return parser.getConfigurationModel();
	}
	
	private BeanDefinitionRegistry renderModelAsBeanDefinitions(ConfigurationModel configModel) {
	    return new ConfigurationModelBeanDefinitionReader().loadBeanDefinitions(configModel);
    }

}
