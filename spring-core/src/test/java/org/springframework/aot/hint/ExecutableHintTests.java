/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.hint;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExecutableHint}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class ExecutableHintTests {

	@Test
	void builtWithAppliesMode() {
		ExecutableHint.Builder builder = new ExecutableHint.Builder("test", Collections.emptyList());
		assertThat(builder.build().getMode()).isEqualTo(ExecutableMode.INVOKE);
		ExecutableHint.builtWith(ExecutableMode.INTROSPECT).accept(builder);
		assertThat(builder.build().getMode()).isEqualTo(ExecutableMode.INTROSPECT);
	}

}
