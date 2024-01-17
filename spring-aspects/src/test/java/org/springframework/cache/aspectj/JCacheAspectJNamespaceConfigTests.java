/*
 * Copyright 2002-2024 the original author or authors.
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
