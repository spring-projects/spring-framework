/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jndi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
class JndiTemplateEditorTests {

	@Test
	void nullIsIllegalArgument() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new JndiTemplateEditor().setAsText(null));
	}

	@Test
	void emptyStringMeansNullEnvironment() {
		JndiTemplateEditor je = new JndiTemplateEditor();
		je.setAsText("");
		JndiTemplate jt = (JndiTemplate) je.getValue();
		assertThat(jt.getEnvironment()).isNull();
	}

	@Test
	void customEnvironment() {
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
