/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.view.script;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScriptTemplateViewResolver}.
 *
 * @author Sebastien Deleuze
 */
class ScriptTemplateViewResolverTests {

	@Test
	void viewClass() {
		ScriptTemplateViewResolver resolver = new ScriptTemplateViewResolver();
		assertThat(resolver.requiredViewClass()).isEqualTo(ScriptTemplateView.class);
		DirectFieldAccessor viewAccessor = new DirectFieldAccessor(resolver);
		Class<?> viewClass = (Class<?>) viewAccessor.getPropertyValue("viewClass");
		assertThat(viewClass).isEqualTo(ScriptTemplateView.class);
	}

}
