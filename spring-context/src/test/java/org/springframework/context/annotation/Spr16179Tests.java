/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Oliver Gierke
 */
class Spr16179Tests {

	@Test
	void repro() {
		try (AnnotationConfigApplicationContext bf = new AnnotationConfigApplicationContext(AssemblerConfig.class, AssemblerInjection.class)) {
			assertThat(bf.getBean(AssemblerInjection.class).assembler0).isSameAs(bf.getBean("someAssembler"));
			assertThat(bf.getBean(AssemblerInjection.class).assembler1).isNull();
			assertThat(bf.getBean(AssemblerInjection.class).assembler2).isSameAs(bf.getBean("pageAssembler"));
			assertThat(bf.getBean(AssemblerInjection.class).assembler3).isSameAs(bf.getBean("pageAssembler"));
			assertThat(bf.getBean(AssemblerInjection.class).assembler4).isSameAs(bf.getBean("pageAssembler"));
			assertThat(bf.getBean(AssemblerInjection.class).assembler5).isSameAs(bf.getBean("pageAssembler"));
			assertThat(bf.getBean(AssemblerInjection.class).assembler6).isSameAs(bf.getBean("pageAssembler"));
			assertThat(bf.getBean(AssemblerInjection.class).assembler7).isSameAs(bf.getBean("pageAssembler"));
		}
	}


	@Configuration
	static class AssemblerConfig {

		@Bean
		PageAssemblerImpl<?> pageAssembler() {
			return new PageAssemblerImpl<>();
		}

		@Bean
		Assembler<SomeType> someAssembler() {
			return new Assembler<>() {};
		}
	}


	public static class AssemblerInjection {

		@Autowired(required = false)
		Assembler<SomeType> assembler0;

		@Autowired(required = false)
		Assembler<SomeOtherType> assembler1;

		@Autowired(required = false)
		Assembler<Page<String>> assembler2;

		@Autowired(required = false)
		@SuppressWarnings("rawtypes")
		Assembler<Page> assembler3;

		@Autowired(required = false)
		Assembler<Page<?>> assembler4;

		@Autowired(required = false)
		PageAssembler<?> assembler5;

		@Autowired(required = false)
		PageAssembler<String> assembler6;

		@Autowired(required = false)
		PageAssembler<String[]> assembler7;
	}


	interface Assembler<T> {}

	interface PageAssembler<T> extends Assembler<Page<T>> {}

	static class PageAssemblerImpl<T> implements PageAssembler<T> {}

	interface Page<T> {}

	interface SomeType {}

	interface SomeOtherType {}

}
