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

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Selectively imports configuration required to configure Tiles, Freemarker, or
 * Velocity for view resolution depending on whether those 3rd party libraries
 * are available on the classpath.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.1
 *
 * @see WebMvcFreeMarkerConfiguration
 */
public class ViewConfigurationImportSelector implements DeferredImportSelector {

	private static final boolean tilesPresent = ClassUtils.isPresent(
			"org.apache.tiles.startup.TilesInitializer", ViewConfigurationImportSelector.class.getClassLoader());

	private static final boolean velocityPresent = ClassUtils.isPresent(
			"org.apache.velocity.app.VelocityEngine", ViewConfigurationImportSelector.class.getClassLoader());

	private static final boolean freeMarkerPresent = ClassUtils.isPresent(
			"freemarker.template.Configuration", ViewConfigurationImportSelector.class.getClassLoader());


	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		List<String> classes = new ArrayList<String>(3);
		if (tilesPresent) {
			classes.add(WebMvcTilesConfiguration.class.getName());
		}
		if (velocityPresent) {
			classes.add(WebMvcVelocityConfiguration.class.getName());
		}
		if (freeMarkerPresent) {
			classes.add(WebMvcFreeMarkerConfiguration.class.getName());
		}
		return classes.toArray(new String[0]);
	}

}
