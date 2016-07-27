/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.componentscan.importing.ImportingConfig;
import org.springframework.context.annotation.componentscan.simple.SimpleComponent;

/**
 * Tests covering overlapping use of @ComponentScan and @Import annotations.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class ComponentScanAndImportAnnotationInteractionTests {

	@Test
	public void componentScanOverlapsWithImport() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Config1.class);
		ctx.register(Config2.class);
		ctx.refresh(); // no conflicts found trying to register SimpleComponent
		ctx.getBean(SimpleComponent.class); // succeeds -> there is only one bean of type SimpleComponent
	}

	@Test
	public void componentScanOverlapsWithImportUsingAsm() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("config1", new RootBeanDefinition(Config1.class.getName()));
		ctx.registerBeanDefinition("config2", new RootBeanDefinition(Config2.class.getName()));
		ctx.refresh(); // no conflicts found trying to register SimpleComponent
		ctx.getBean(SimpleComponent.class); // succeeds -> there is only one bean of type SimpleComponent
	}

	@Test
	public void componentScanViaImport() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Config3.class);
		ctx.refresh();
		ctx.getBean(SimpleComponent.class);
	}

	@Test
	public void componentScanViaImportUsingAsm() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("config", new RootBeanDefinition(Config3.class.getName()));
		ctx.refresh();
		ctx.getBean(SimpleComponent.class);
	}

	@Test
	public void componentScanViaImportUsingScan() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("org.springframework.context.annotation.componentscan.importing");
		ctx.refresh();
		ctx.getBean(SimpleComponent.class);
	}

	@Test
	public void circularImportViaComponentScan() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("config", new RootBeanDefinition(ImportingConfig.class.getName()));
		ctx.refresh();
		ctx.getBean(SimpleComponent.class);
	}


	@ComponentScan("org.springframework.context.annotation.componentscan.simple")
	static final class Config1 {
	}


	@Import(org.springframework.context.annotation.componentscan.simple.SimpleComponent.class)
	static final class Config2 {
	}


	@Import(ImportedConfig.class)
	static final class Config3 {
	}


	@ComponentScan({"org.springframework.context.annotation.componentscan.simple",
			"org.springframework.context.annotation.componentscan.importing"})
	public static final class ImportedConfig {
	}

}
