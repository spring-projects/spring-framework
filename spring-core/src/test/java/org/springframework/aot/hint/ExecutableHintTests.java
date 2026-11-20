/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
@SuppressWarnings("removal")
class ExecutableHintTests {

	@Test
	void builtWithAppliesMode() {
		ExecutableHint.Builder builder = new ExecutableHint.Builder("test", Collections.emptyList());
		assertThat(builder.build().getMode()).isEqualTo(ExecutableMode.INVOKE);
		ExecutableHint.builtWith(ExecutableMode.INTROSPECT).accept(builder);
		assertThat(builder.build().getMode()).isEqualTo(ExecutableMode.INTROSPECT);
	}

}
