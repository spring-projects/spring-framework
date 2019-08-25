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

package org.springframework.beans.factory.wiring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the BeanWiringInfo class.
 *
 * @author Rick Evans
 * @author Sam Brannen
 */
public class BeanWiringInfoTests {

	@Test
	public void ctorWithNullBeanName() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BeanWiringInfo(null));
	}

	@Test
	public void ctorWithWhitespacedBeanName() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BeanWiringInfo("   \t"));
	}

	@Test
	public void ctorWithEmptyBeanName() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BeanWiringInfo(""));
	}

	@Test
	public void ctorWithNegativeIllegalAutowiringValue() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BeanWiringInfo(-1, true));
	}

	@Test
	public void ctorWithPositiveOutOfRangeAutowiringValue() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BeanWiringInfo(123871, true));
	}

	@Test
	public void usingAutowireCtorIndicatesAutowiring() throws Exception {
		BeanWiringInfo info = new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_NAME, true);
		assertThat(info.indicatesAutowiring()).isTrue();
	}

	@Test
	public void usingBeanNameCtorDoesNotIndicateAutowiring() throws Exception {
		BeanWiringInfo info = new BeanWiringInfo("fooService");
		assertThat(info.indicatesAutowiring()).isFalse();
	}

	@Test
	public void noDependencyCheckValueIsPreserved() throws Exception {
		BeanWiringInfo info = new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_NAME, true);
		assertThat(info.getDependencyCheck()).isTrue();
	}

	@Test
	public void dependencyCheckValueIsPreserved() throws Exception {
		BeanWiringInfo info = new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_TYPE, false);
		assertThat(info.getDependencyCheck()).isFalse();
	}

}
