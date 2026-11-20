/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author Rick Evans
 */
class RefreshableScriptTargetSourceTests {

	@Test
	void createWithNullScriptSource() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new RefreshableScriptTargetSource(mock(), "a.bean", null, null, false));
	}

}
