/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

import org.jspecify.annotations.Nullable;

/**
 * Class describing a cache 'cacheable' operation.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Marcin Kamionowski
 * @since 3.1
 */
public class CacheableOperation extends CacheOperation {

	private final @Nullable String unless;

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


	public @Nullable String getUnless() {
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

		private @Nullable String unless;

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
			sb.append('\'');
			sb.append(" | sync='");
			sb.append(this.sync);
			sb.append('\'');
			return sb;
		}

		@Override
		public CacheableOperation build() {
			return new CacheableOperation(this);
		}
	}

}
