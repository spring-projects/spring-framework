/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.aspectj;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.contextsupport.testfixture.jcache.AbstractJCacheAnnotationTests;

/**
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class JCacheAspectJNamespaceConfigTests extends AbstractJCacheAnnotationTests {

	@Override
	protected ApplicationContext getApplicationContext() {
		GenericXmlApplicationContext context = new GenericXmlApplicationContext();
		// Disallow bean definition overriding to test https://github.com/spring-projects/spring-framework/pull/27499
		context.setAllowBeanDefinitionOverriding(false);
		context.load("/org/springframework/cache/config/annotation-jcache-aspectj.xml");
		context.refresh();
		return context;
	}

}
