/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableLoadTimeWeaving.AspectJWeaving;
import org.springframework.context.weaving.AspectJWeavingEnabler;
import org.springframework.context.weaving.DefaultContextLoadTimeWeaver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.util.Assert;

import static org.springframework.context.weaving.AspectJWeavingEnabler.*;

/**
 * {@code @Configuration} class that registers a {@link LoadTimeWeaver} bean.
 *
 * <p>This configuration class is automatically imported when using the @{@link
 * EnableLoadTimeWeaving} annotation.  See {@code @EnableLoadTimeWeaving} Javadoc for
 * complete usage details.
 *
 * @author Chris Beams
 * @since 3.1
 * @see LoadTimeWeavingConfigurer
 * @see ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 */
@Configuration
public class LoadTimeWeavingConfiguration implements ImportAware, BeanClassLoaderAware {

	private AnnotationAttributes enableLTW;

	@Autowired(required=false)
	private LoadTimeWeavingConfigurer ltwConfigurer;

	private ClassLoader beanClassLoader;

	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableLTW = MetadataUtils.attributesFor(importMetadata, EnableLoadTimeWeaving.class);
		Assert.notNull(this.enableLTW,
				"@EnableLoadTimeWeaving is not present on importing class " +
				importMetadata.getClassName());
	}

	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	@Bean(name=ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public LoadTimeWeaver loadTimeWeaver() {
		LoadTimeWeaver loadTimeWeaver = null;

		if (ltwConfigurer != null) {
			// the user has provided a custom LTW instance
			loadTimeWeaver = ltwConfigurer.getLoadTimeWeaver();
		}

		if (loadTimeWeaver == null) {
			// no custom LTW provided -> fall back to the default
			loadTimeWeaver = new DefaultContextLoadTimeWeaver(this.beanClassLoader);
		}

		AspectJWeaving aspectJWeaving = this.enableLTW.getEnum("aspectjWeaving");
		switch (aspectJWeaving) {
			case DISABLED:
				// AJ weaving is disabled -> do nothing
				break;
			case AUTODETECT:
				if (this.beanClassLoader.getResource(ASPECTJ_AOP_XML_RESOURCE) == null) {
					// No aop.xml present on the classpath -> treat as 'disabled'
					break;
				}
				// aop.xml is present on the classpath -> fall through and enable
			case ENABLED:
				AspectJWeavingEnabler.enableAspectJWeaving(loadTimeWeaver, this.beanClassLoader);
		}

		return loadTimeWeaver;
	}

}
