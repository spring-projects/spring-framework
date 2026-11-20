/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.xml;

import java.io.StringWriter;

import org.assertj.core.api.AssertProvider;
import org.xmlunit.assertj.XmlAssert;

/**
 * {@link AssertProvider} to allow XML content assertions. Ultimately delegates
 * to {@link XmlAssert}.
 *
 * @author Phillip Webb
 */
public class XmlContent implements AssertProvider<XmlContentAssert> {

	private final Object source;

	private XmlContent(Object source) {
		this.source = source;
	}

	@Override
	public XmlContentAssert assertThat() {
		return new XmlContentAssert(this.source);
	}

	public static XmlContent from(Object source) {
		return of(source);
	}

	public static XmlContent of(Object source) {
		if (source instanceof StringWriter) {
			return of(source.toString());
		}
		return new XmlContent(source);
	}

}
