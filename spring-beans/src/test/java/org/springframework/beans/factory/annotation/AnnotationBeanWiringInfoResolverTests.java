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

package org.springframework.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.wiring.BeanWiringInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
class AnnotationBeanWiringInfoResolverTests {

	@Test
	void testResolveWiringInfo() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new AnnotationBeanWiringInfoResolver().resolveWiringInfo(null));
	}

	@Test
	void testResolveWiringInfoWithAnInstanceOfANonAnnotatedClass() {
		AnnotationBeanWiringInfoResolver resolver = new AnnotationBeanWiringInfoResolver();
		BeanWiringInfo info = resolver.resolveWiringInfo("java.lang.String is not @Configurable");
		assertThat(info).as("Must be returning null for a non-@Configurable class instance").isNull();
	}

	@Test
	void testResolveWiringInfoWithAnInstanceOfAnAnnotatedClass() {
		AnnotationBeanWiringInfoResolver resolver = new AnnotationBeanWiringInfoResolver();
		BeanWiringInfo info = resolver.resolveWiringInfo(new Soap());
		assertThat(info).as("Must *not* be returning null for a non-@Configurable class instance").isNotNull();
	}

	@Test
	void testResolveWiringInfoWithAnInstanceOfAnAnnotatedClassWithAutowiringTurnedOffExplicitly() {
		AnnotationBeanWiringInfoResolver resolver = new AnnotationBeanWiringInfoResolver();
		BeanWiringInfo info = resolver.resolveWiringInfo(new WirelessSoap());
		assertThat(info).as("Must *not* be returning null for an @Configurable class instance even when autowiring is NO").isNotNull();
		assertThat(info.indicatesAutowiring()).isFalse();
		assertThat(info.getBeanName()).isEqualTo(WirelessSoap.class.getName());
	}

	@Test
	void testResolveWiringInfoWithAnInstanceOfAnAnnotatedClassWithAutowiringTurnedOffExplicitlyAndCustomBeanName() {
		AnnotationBeanWiringInfoResolver resolver = new AnnotationBeanWiringInfoResolver();
		BeanWiringInfo info = resolver.resolveWiringInfo(new NamedWirelessSoap());
		assertThat(info).as("Must *not* be returning null for an @Configurable class instance even when autowiring is NO").isNotNull();
		assertThat(info.indicatesAutowiring()).isFalse();
		assertThat(info.getBeanName()).isEqualTo("DerBigStick");
	}


	@Configurable()
	private static class Soap {
	}


	@Configurable(autowire = Autowire.NO)
	private static class WirelessSoap {
	}


	@Configurable(autowire = Autowire.NO, value = "DerBigStick")
	private static class NamedWirelessSoap {
	}

}
