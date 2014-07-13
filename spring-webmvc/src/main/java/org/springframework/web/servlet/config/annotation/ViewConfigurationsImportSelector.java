/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This class imports @{@link org.springframework.context.annotation.Configuration}
 * classes for view configurers based on a classpath criteria.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class ViewConfigurationsImportSelector implements ImportSelector {

	private static final boolean tilesPresent =
			ClassUtils.isPresent("org.apache.tiles.startup.TilesInitializer", ViewConfigurationsImportSelector.class.getClassLoader());

	private static final boolean velocityPresent =
			ClassUtils.isPresent("org.apache.velocity.app.VelocityEngine", ViewConfigurationsImportSelector.class.getClassLoader());

	private static final boolean freeMarkerPresent =
			ClassUtils.isPresent("freemarker.template.Configuration", ViewConfigurationsImportSelector.class.getClassLoader());

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		List<String> configurationClasses = new ArrayList<String>();
		if(tilesPresent) {
			configurationClasses.add(TilesConfigurerConfigurationSupport.class.getName());
		}
		if(velocityPresent) {
			configurationClasses.add(VelocityConfigurerConfigurationSupport.class.getName());
		}
		if(freeMarkerPresent) {
			configurationClasses.add(FreeMarkerConfigurerConfigurationSupport.class.getName());
		}
		return configurationClasses.toArray(new String[0]);
	}
}
