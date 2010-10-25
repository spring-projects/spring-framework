/*
 * Copyright 2002-2010 the original author or authors.
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

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.context.annotation.ComponentScan.ExcludeFilter;
import org.springframework.context.annotation.ComponentScan.IncludeFilter;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Unit tests for {@link ComponentScan} annotation.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class ComponentScanAnnotationTests {
	@Test
	public void test() {

	}
}

@interface MyAnnotation { }

@Configuration
@ComponentScan(
	packageOf={TestBean.class},
	nameGenerator = DefaultBeanNameGenerator.class,
	scopedProxy = ScopedProxyMode.NO,
	scopeResolver = AnnotationScopeMetadataResolver.class,
	useDefaultFilters = false,
	resourcePattern = "**/*custom.class",
	includeFilters = {
		@IncludeFilter(type = FilterType.ANNOTATION, value = MyAnnotation.class)
	},
	excludeFilters = {
		@ExcludeFilter(type = FilterType.CUSTOM, value = TypeFilter.class)
	}
)
class MyConfig {

}

@ComponentScan(packageOf=example.scannable.NamedComponent.class)
class SimpleConfig { }