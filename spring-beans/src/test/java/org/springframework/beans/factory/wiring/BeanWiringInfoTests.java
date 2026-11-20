/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.wiring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BeanWiringInfo}.
 *
 * @author Rick Evans
 * @author Sam Brannen
 */
class BeanWiringInfoTests {

	@Test
	void ctorWithNullBeanName() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BeanWiringInfo(null));
	}

	@Test
	void ctorWithWhitespacedBeanName() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BeanWiringInfo("   \t"));
	}

	@Test
	void ctorWithEmptyBeanName() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BeanWiringInfo(""));
	}

	@Test
	void ctorWithNegativeIllegalAutowiringValue() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BeanWiringInfo(-1, true));
	}

	@Test
	void ctorWithPositiveOutOfRangeAutowiringValue() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BeanWiringInfo(123871, true));
	}

	@Test
	void usingAutowireCtorIndicatesAutowiring() {
		BeanWiringInfo info = new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_NAME, true);
		assertThat(info.indicatesAutowiring()).isTrue();
	}

	@Test
	void usingBeanNameCtorDoesNotIndicateAutowiring() {
		BeanWiringInfo info = new BeanWiringInfo("fooService");
		assertThat(info.indicatesAutowiring()).isFalse();
	}

	@Test
	void noDependencyCheckValueIsPreserved() {
		BeanWiringInfo info = new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_NAME, true);
		assertThat(info.getDependencyCheck()).isTrue();
	}

	@Test
	void dependencyCheckValueIsPreserved() {
		BeanWiringInfo info = new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_TYPE, false);
		assertThat(info.getDependencyCheck()).isFalse();
	}

}
