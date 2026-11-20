/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.view.xslt;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @since 2.0
 */
class XsltViewResolverTests {

	@Test
	void resolveView() throws Exception {
		StaticApplicationContext ctx = new StaticApplicationContext();

		String prefix = ClassUtils.classPackageAsResourcePath(getClass());
		String suffix = ".xsl";
		String viewName = "products";

		XsltViewResolver viewResolver = new XsltViewResolver();
		viewResolver.setPrefix(prefix);
		viewResolver.setSuffix(suffix);
		viewResolver.setApplicationContext(ctx);

		XsltView view = (XsltView) viewResolver.resolveViewName(viewName, Locale.ENGLISH);
		assertThat(view).as("View should not be null").isNotNull();
		assertThat(view.getUrl()).as("Incorrect URL").isEqualTo((prefix + viewName + suffix));
	}
}
