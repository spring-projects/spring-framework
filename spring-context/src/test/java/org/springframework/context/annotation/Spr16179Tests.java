/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Oliver Gierke
 */
public class Spr16179Tests {

	@Test
	public void repro() {
		AnnotationConfigApplicationContext bf =
				new AnnotationConfigApplicationContext(AssemblerConfig.class, AssemblerInjection.class);

		assertThat(bf.getBean(AssemblerInjection.class).assembler0).isSameAs(bf.getBean("someAssembler"));
		// assertNull(bf.getBean(AssemblerInjection.class).assembler1);  TODO: accidental match
		// assertNull(bf.getBean(AssemblerInjection.class).assembler2);
		assertThat(bf.getBean(AssemblerInjection.class).assembler3).isSameAs(bf.getBean("pageAssembler"));
		assertThat(bf.getBean(AssemblerInjection.class).assembler4).isSameAs(bf.getBean("pageAssembler"));
		assertThat(bf.getBean(AssemblerInjection.class).assembler5).isSameAs(bf.getBean("pageAssembler"));
		assertThat(bf.getBean(AssemblerInjection.class).assembler6).isSameAs(bf.getBean("pageAssembler"));
	}


	@Configuration
	static class AssemblerConfig {

		@Bean
		PageAssemblerImpl<?> pageAssembler() {
			return new PageAssemblerImpl<>();
		}

		@Bean
		Assembler<SomeType> someAssembler() {
			return new Assembler<SomeType>() {};
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
	}


	interface Assembler<T> {}

	interface PageAssembler<T> extends Assembler<Page<T>> {}

	static class PageAssemblerImpl<T> implements PageAssembler<T> {}

	interface Page<T> {}

	interface SomeType {}

	interface SomeOtherType {}

}
