/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context;

import java.io.Serializable;

/**
 * Key object to the {@link ContextCache}.
 *
 * Consists of two {@link MergedContextConfiguration}. one for current, and another for parent context.
 *
 * @author Tadaya Tsuyukubo
 * @since 3.2
 */
class ContextCacheKey implements Serializable {

	private static final long serialVersionUID = 2230287909396051971L;

	private MergedContextConfiguration mergedContextConfiguration;

	private MergedContextConfiguration parentMergedContextConfiguration;

	ContextCacheKey(MergedContextConfiguration mergedContextConfiguration,
			MergedContextConfiguration parentMergedContextConfiguration) {
		this.mergedContextConfiguration = mergedContextConfiguration;
		this.parentMergedContextConfiguration = parentMergedContextConfiguration;
	}

	public MergedContextConfiguration getMergedContextConfiguration() {
		return mergedContextConfiguration;
	}

	public void setMergedContextConfiguration(MergedContextConfiguration mergedContextConfiguration) {
		this.mergedContextConfiguration = mergedContextConfiguration;
	}

	public MergedContextConfiguration getParentMergedContextConfiguration() {
		return parentMergedContextConfiguration;
	}

	public void setParentMergedContextConfiguration(MergedContextConfiguration parentMergedContextConfiguration) {
		this.parentMergedContextConfiguration = parentMergedContextConfiguration;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ContextCacheKey key = (ContextCacheKey) o;

		if (mergedContextConfiguration != null ? !mergedContextConfiguration.equals(key.mergedContextConfiguration) :
				key.mergedContextConfiguration != null) {
			return false;
		}
		if (parentMergedContextConfiguration != null ?
				!parentMergedContextConfiguration.equals(key.parentMergedContextConfiguration) :
				key.parentMergedContextConfiguration != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = mergedContextConfiguration != null ? mergedContextConfiguration.hashCode() : 0;
		result = 31 * result +
				(parentMergedContextConfiguration != null ? parentMergedContextConfiguration.hashCode() : 0);
		return result;
	}
}
