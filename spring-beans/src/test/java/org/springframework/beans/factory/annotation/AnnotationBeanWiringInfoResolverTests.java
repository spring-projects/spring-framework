/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void resolveWiringInfo() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new AnnotationBeanWiringInfoResolver().resolveWiringInfo(null));
	}

	@Test
	void resolveWiringInfoWithAnInstanceOfANonAnnotatedClass() {
		AnnotationBeanWiringInfoResolver resolver = new AnnotationBeanWiringInfoResolver();
		BeanWiringInfo info = resolver.resolveWiringInfo("java.lang.String is not @Configurable");
		assertThat(info).as("Must be returning null for a non-@Configurable class instance").isNull();
	}

	@Test
	void resolveWiringInfoWithAnInstanceOfAnAnnotatedClass() {
		AnnotationBeanWiringInfoResolver resolver = new AnnotationBeanWiringInfoResolver();
		BeanWiringInfo info = resolver.resolveWiringInfo(new Soap());
		assertThat(info).as("Must *not* be returning null for a non-@Configurable class instance").isNotNull();
	}

	@Test
	void resolveWiringInfoWithAnInstanceOfAnAnnotatedClassWithAutowiringTurnedOffExplicitly() {
		AnnotationBeanWiringInfoResolver resolver = new AnnotationBeanWiringInfoResolver();
		BeanWiringInfo info = resolver.resolveWiringInfo(new WirelessSoap());
		assertThat(info).as("Must *not* be returning null for an @Configurable class instance even when autowiring is NO").isNotNull();
		assertThat(info.indicatesAutowiring()).isFalse();
		assertThat(info.getBeanName()).isEqualTo(WirelessSoap.class.getName());
	}

	@Test
	void resolveWiringInfoWithAnInstanceOfAnAnnotatedClassWithAutowiringTurnedOffExplicitlyAndCustomBeanName() {
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
