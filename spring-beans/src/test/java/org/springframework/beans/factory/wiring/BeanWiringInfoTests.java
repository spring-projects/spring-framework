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
