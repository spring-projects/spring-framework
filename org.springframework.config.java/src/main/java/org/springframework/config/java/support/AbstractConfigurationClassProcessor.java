package org.springframework.config.java.support;

import static org.springframework.util.StringUtils.*;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.config.java.ConfigurationModel;
import org.springframework.config.java.internal.parsing.ConfigurationParser;

public abstract class AbstractConfigurationClassProcessor {
	
	static String CGLIB_PACKAGE = "net.sf.cglib.proxy";

	protected abstract BeanDefinitionRegistry getConfigurationBeanDefinitions(boolean includeAbstractBeanDefs);
	
	protected abstract ConfigurationParser createConfigurationParser();
	
	protected abstract void validateModel(ConfigurationModel configModel);
	
	protected BeanDefinitionRegistry processConfigBeanDefinitions() {
		BeanDefinitionRegistry configBeanDefs = getConfigurationBeanDefinitions(false);
		
		if(configBeanDefs.getBeanDefinitionCount() == 0)
			return configBeanDefs; // nothing to do - don't waste any more cycles
		
		// TODO: the location of this cglib check is temporary, pending removal of the
		// @FactoryMethod meta-annotation indirection
		if(Package.getPackage(CGLIB_PACKAGE) == null)
			throw new RuntimeException("CGLIB is required to process @Configuration classes. " +
					"Either add CGLIB v2.2.3 to the classpath or remove the following " +
					"@Configuration bean definitions: ["
					+ arrayToCommaDelimitedString(configBeanDefs.getBeanDefinitionNames()) + "]");
		
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
