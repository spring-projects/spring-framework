/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.xml;

import org.assertj.core.api.AbstractAssert;
import org.w3c.dom.Node;
import org.xmlunit.assertj.XmlAssert;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.NodeMatcher;
import org.xmlunit.util.Predicate;

/**
 * Assertions exposed by {@link XmlContent}.
 *
 * @author Phillip Webb
 */
public class XmlContentAssert extends AbstractAssert<XmlContentAssert, Object> {

	XmlContentAssert(Object actual) {
		super(actual, XmlContentAssert.class);
	}

	public XmlContentAssert isSimilarTo(Object control) {
		XmlAssert.assertThat(super.actual).and(control).areSimilar();
		return this;
	}

	public XmlContentAssert isSimilarTo(Object control, Predicate<Node> nodeFilter) {
		XmlAssert.assertThat(super.actual).and(control).withNodeFilter(nodeFilter).areSimilar();
		return this;
	}

	public XmlContentAssert isSimilarTo(String control,
			DifferenceEvaluator differenceEvaluator) {
		XmlAssert.assertThat(super.actual).and(control).withDifferenceEvaluator(
				differenceEvaluator).areSimilar();
		return this;
	}

	public XmlContentAssert isSimilarToIgnoringWhitespace(Object control) {
		XmlAssert.assertThat(super.actual).and(control).ignoreWhitespace().areSimilar();
		return this;
	}


	public XmlContentAssert isSimilarToIgnoringWhitespace(String control, NodeMatcher nodeMatcher) {
		XmlAssert.assertThat(super.actual).and(control).ignoreWhitespace().withNodeMatcher(nodeMatcher).areSimilar();
		return this;
	}

}
