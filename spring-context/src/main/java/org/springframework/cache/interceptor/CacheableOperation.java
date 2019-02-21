/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cache.interceptor;

import org.springframework.lang.Nullable;

/**
 * Class describing a cache 'cacheable' operation.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Marcin Kamionowski
 * @since 3.1
 */
public class CacheableOperation extends CacheOperation {

	@Nullable
	private final String unless;

	private final boolean sync;


	/**
	 * Create a new {@link CacheableOperation} instance from the given builder.
	 * @since 4.3
	 */
	public CacheableOperation(CacheableOperation.Builder b) {
		super(b);
		this.unless = b.unless;
		this.sync = b.sync;
	}


	@Nullable
	public String getUnless() {
		return this.unless;
	}

	public boolean isSync() {
		return this.sync;
	}


	/**
	 * A builder that can be used to create a {@link CacheableOperation}.
	 * @since 4.3
	 */
	public static class Builder extends CacheOperation.Builder {

		@Nullable
		private String unless;

		private boolean sync;

		public void setUnless(String unless) {
			this.unless = unless;
		}

		public void setSync(boolean sync) {
			this.sync = sync;
		}

		@Override
		protected StringBuilder getOperationDescription() {
			StringBuilder sb = super.getOperationDescription();
			sb.append(" | unless='");
			sb.append(this.unless);
			sb.append("'");
			sb.append(" | sync='");
			sb.append(this.sync);
			sb.append("'");
			return sb;
		}

		@Override
		public CacheableOperation build() {
			return new CacheableOperation(this);
		}
	}

}
