/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.support;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.test.util.XpathExpectationsHelper;
import org.springframework.util.MimeType;

/**
 * Base class for applying XPath assertions in RestTestClient and WebTestClient.
 *
 * @author Rob Worsnop
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <B> the type of body spec (RestTestClient vs WebTestClient specific)
 */
public abstract class AbstractXpathAssertions<B> {

	private final B bodySpec;

	private final XpathExpectationsHelper xpathHelper;


	public AbstractXpathAssertions(
			B spec, String expression, @Nullable Map<String, String> namespaces, Object... args) {

		this.bodySpec = spec;
		this.xpathHelper = initXpathHelper(expression, namespaces, args);
	}

	private static XpathExpectationsHelper initXpathHelper(
			String expression, @Nullable Map<String, String> namespaces, Object[] args) {

		try {
			return new XpathExpectationsHelper(expression, namespaces, args);
		}
		catch (XPathExpressionException ex) {
			throw new AssertionError("XML parsing error", ex);
		}
	}


	/**
	 * Return the body spec.
	 */
	protected B getBodySpec() {
		return this.bodySpec;
	}

	/**
	 * Subclasses must implement this to provide access to response headers.
	 */
	protected abstract Optional<HttpHeaders> getResponseHeaders();

	/**
	 * Subclasses must implement this to provide access to the response content.
	 */
	protected abstract byte[] getContent();


	/**
	 * Delegates to {@link XpathExpectationsHelper#assertString(byte[], String, String)}.
	 */
	public B isEqualTo(String expectedValue) {
		return assertWith(() -> this.xpathHelper.assertString(getContent(), getCharset(), expectedValue));
	}

	/**
	 * Delegates to {@link XpathExpectationsHelper#assertNumber(byte[], String, Double)}.
	 */
	public B isEqualTo(Double expectedValue) {
		return assertWith(() -> this.xpathHelper.assertNumber(getContent(), getCharset(), expectedValue));
	}

	/**
	 * Delegates to {@link XpathExpectationsHelper#assertBoolean(byte[], String, boolean)}.
	 */
	public B isEqualTo(boolean expectedValue) {
		return assertWith(() -> this.xpathHelper.assertBoolean(getContent(), getCharset(), expectedValue));
	}

	/**
	 * Delegates to {@link XpathExpectationsHelper#exists(byte[], String)}.
	 */
	public B exists() {
		return assertWith(() -> this.xpathHelper.exists(getContent(), getCharset()));
	}

	/**
	 * Delegates to {@link XpathExpectationsHelper#doesNotExist(byte[], String)}.
	 */
	public B doesNotExist() {
		return assertWith(() -> this.xpathHelper.doesNotExist(getContent(), getCharset()));
	}

	/**
	 * Delegates to {@link XpathExpectationsHelper#assertNodeCount(byte[], String, int)}.
	 */
	public B nodeCount(int expectedCount) {
		return assertWith(() -> this.xpathHelper.assertNodeCount(getContent(), getCharset(), expectedCount));
	}

	/**
	 * Delegates to {@link XpathExpectationsHelper#assertString(byte[], String, Matcher)}.
	 */
	public B string(Matcher<? super String> matcher){
		return assertWith(() -> this.xpathHelper.assertString(getContent(), getCharset(), matcher));
	}

	/**
	 * Delegates to {@link XpathExpectationsHelper#assertNumber(byte[], String, Matcher)}.
	 */
	public B number(Matcher<? super Double> matcher){
		return assertWith(() -> this.xpathHelper.assertNumber(getContent(), getCharset(), matcher));
	}

	/**
	 * Delegates to {@link XpathExpectationsHelper#assertNodeCount(byte[], String, Matcher)}.
	 */
	public B nodeCount(Matcher<? super Integer> matcher){
		return assertWith(() -> this.xpathHelper.assertNodeCount(getContent(), getCharset(), matcher));
	}

	/**
	 * Consume the result of the XPath evaluation as a String.
	 */
	public B string(Consumer<String> consumer){
		return assertWith(() -> {
			String value = this.xpathHelper.evaluateXpath(getContent(), getCharset(), String.class);
			consumer.accept(value);
		});
	}

	/**
	 * Consume the result of the XPath evaluation as a Double.
	 */
	public B number(Consumer<Double> consumer){
		return assertWith(() -> {
			Double value = this.xpathHelper.evaluateXpath(getContent(), getCharset(), Double.class);
			consumer.accept(value);
		});
	}

	/**
	 * Consume the count of nodes as result of the XPath evaluation.
	 */
	public B nodeCount(Consumer<Integer> consumer){
		return assertWith(() -> {
			Integer value = this.xpathHelper.evaluateXpath(getContent(), getCharset(), Integer.class);
			consumer.accept(value);
		});
	}

	private B assertWith(CheckedExceptionTask task) {
		try {
			task.run();
		}
		catch (Exception ex) {
			throw new AssertionError("XML parsing error", ex);
		}
		return this.bodySpec;
	}

	private String getCharset() {
		return getResponseHeaders()
				.map(HttpHeaders::getContentType)
				.map(MimeType::getCharset)
				.orElse(StandardCharsets.UTF_8)
				.name();
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		throw new AssertionError("Object#equals is disabled " +
				"to avoid being used in error instead of XPathAssertions#isEqualTo(String).");
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}


	/**
	 * Lets us be able to use lambda expressions that could throw checked exceptions, since
	 * {@link XpathExpectationsHelper} throws {@link Exception} on its methods.
	 */
	private interface CheckedExceptionTask {

		void run() throws Exception;

	}
}
