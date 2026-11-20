/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.componentscan.importing.ImportingConfig;
import org.springframework.context.annotation.componentscan.simple.SimpleComponent;

/**
 * Tests covering overlapping use of @ComponentScan and @Import annotations.
 *
 * @author Chris Beams
 * @since 3.1
 */
class ComponentScanAndImportAnnotationInteractionTests {

	@Test
	void componentScanOverlapsWithImport() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Config1.class);
		ctx.register(Config2.class);
		ctx.refresh(); // no conflicts found trying to register SimpleComponent
		ctx.getBean(SimpleComponent.class); // succeeds -> there is only one bean of type SimpleComponent
		ctx.close();
	}

	@Test
	void componentScanOverlapsWithImportUsingAsm() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("config1", new RootBeanDefinition(Config1.class.getName()));
		ctx.registerBeanDefinition("config2", new RootBeanDefinition(Config2.class.getName()));
		ctx.refresh(); // no conflicts found trying to register SimpleComponent
		ctx.getBean(SimpleComponent.class); // succeeds -> there is only one bean of type SimpleComponent
		ctx.close();
	}

	@Test
	void componentScanViaImport() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Config3.class);
		ctx.refresh();
		ctx.getBean(SimpleComponent.class);
		ctx.close();
	}

	@Test
	void componentScanViaImportUsingAsm() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("config", new RootBeanDefinition(Config3.class.getName()));
		ctx.refresh();
		ctx.getBean(SimpleComponent.class);
		ctx.close();
	}

	@Test
	void componentScanViaImportUsingScan() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("org.springframework.context.annotation.componentscan.importing");
		ctx.refresh();
		ctx.getBean(SimpleComponent.class);
		ctx.close();
	}

	@Test
	void circularImportViaComponentScan() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("config", new RootBeanDefinition(ImportingConfig.class.getName()));
		ctx.refresh();
		ctx.getBean(SimpleComponent.class);
		ctx.close();
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

}
