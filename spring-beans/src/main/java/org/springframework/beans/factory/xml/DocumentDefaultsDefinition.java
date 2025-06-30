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

package org.springframework.beans.factory.xml;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.parsing.DefaultsDefinition;

/**
 * Simple JavaBean that holds the defaults specified at the {@code <beans>}
 * level in a standard Spring XML bean definition document:
 * {@code default-lazy-init}, {@code default-autowire}, etc.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 */
public class DocumentDefaultsDefinition implements DefaultsDefinition {

	private @Nullable String lazyInit;

	private @Nullable String merge;

	private @Nullable String autowire;

	private @Nullable String autowireCandidates;

	private @Nullable String initMethod;

	private @Nullable String destroyMethod;

	private @Nullable Object source;


	/**
	 * Set the default lazy-init flag for the document that's currently parsed.
	 */
	public void setLazyInit(@Nullable String lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * Return the default lazy-init flag for the document that's currently parsed.
	 */
	public @Nullable String getLazyInit() {
		return this.lazyInit;
	}

	/**
	 * Set the default merge setting for the document that's currently parsed.
	 */
	public void setMerge(@Nullable String merge) {
		this.merge = merge;
	}

	/**
	 * Return the default merge setting for the document that's currently parsed.
	 */
	public @Nullable String getMerge() {
		return this.merge;
	}

	/**
	 * Set the default autowire setting for the document that's currently parsed.
	 */
	public void setAutowire(@Nullable String autowire) {
		this.autowire = autowire;
	}

	/**
	 * Return the default autowire setting for the document that's currently parsed.
	 */
	public @Nullable String getAutowire() {
		return this.autowire;
	}

	/**
	 * Set the default autowire-candidate pattern for the document that's currently parsed.
	 * Also accepts a comma-separated list of patterns.
	 */
	public void setAutowireCandidates(@Nullable String autowireCandidates) {
		this.autowireCandidates = autowireCandidates;
	}

	/**
	 * Return the default autowire-candidate pattern for the document that's currently parsed.
	 * May also return a comma-separated list of patterns.
	 */
	public @Nullable String getAutowireCandidates() {
		return this.autowireCandidates;
	}

	/**
	 * Set the default init-method setting for the document that's currently parsed.
	 */
	public void setInitMethod(@Nullable String initMethod) {
		this.initMethod = initMethod;
	}

	/**
	 * Return the default init-method setting for the document that's currently parsed.
	 */
	public @Nullable String getInitMethod() {
		return this.initMethod;
	}

	/**
	 * Set the default destroy-method setting for the document that's currently parsed.
	 */
	public void setDestroyMethod(@Nullable String destroyMethod) {
		this.destroyMethod = destroyMethod;
	}

	/**
	 * Return the default destroy-method setting for the document that's currently parsed.
	 */
	public @Nullable String getDestroyMethod() {
		return this.destroyMethod;
	}

	/**
	 * Set the configuration source {@code Object} for this metadata element.
	 * <p>The exact type of the object will depend on the configuration mechanism used.
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	public @Nullable Object getSource() {
		return this.source;
	}

}
