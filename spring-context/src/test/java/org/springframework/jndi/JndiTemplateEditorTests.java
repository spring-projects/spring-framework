/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jndi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class JndiTemplateEditorTests {

	@Test
	public void testNullIsIllegalArgument() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new JndiTemplateEditor().setAsText(null));
	}

	@Test
	public void testEmptyStringMeansNullEnvironment() {
		JndiTemplateEditor je = new JndiTemplateEditor();
		je.setAsText("");
		JndiTemplate jt = (JndiTemplate) je.getValue();
		assertThat(jt.getEnvironment()).isNull();
	}

	@Test
	public void testCustomEnvironment() {
		JndiTemplateEditor je = new JndiTemplateEditor();
		// These properties are meaningless for JNDI, but we don't worry about that:
		// the underlying JNDI implementation will throw exceptions when the user tries
		// to look anything up
		je.setAsText("jndiInitialSomethingOrOther=org.springframework.myjndi.CompleteRubbish\nfoo=bar");
		JndiTemplate jt = (JndiTemplate) je.getValue();
		assertThat(jt.getEnvironment()).hasSize(2);
		assertThat(jt.getEnvironment().getProperty("jndiInitialSomethingOrOther")).isEqualTo(
				"org.springframework.myjndi.CompleteRubbish");
		assertThat(jt.getEnvironment().getProperty("foo")).isEqualTo("bar");
	}

}
