/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

/**
 * Class describing a cache 'evict' operation.
 *
 * @author Costin Leau
 * @author Marcin Kamionowski
 * @since 3.1
 */
public class CacheEvictOperation extends CacheOperation {

	private final boolean cacheWide;

	private final boolean beforeInvocation;


	/**
	 * Create a new {@link CacheEvictOperation} instance from the given builder.
	 * @since 4.3
	 */
	public CacheEvictOperation(CacheEvictOperation.Builder b) {
		super(b);
		this.cacheWide = b.cacheWide;
		this.beforeInvocation = b.beforeInvocation;
	}


	public boolean isCacheWide() {
		return this.cacheWide;
	}

	public boolean isBeforeInvocation() {
		return this.beforeInvocation;
	}


	/**
	 * A builder that can be used to create a {@link CacheEvictOperation}.
	 * @since 4.3
	 */
	public static class Builder extends CacheOperation.Builder {

		private boolean cacheWide = false;

		private boolean beforeInvocation = false;

		public void setCacheWide(boolean cacheWide) {
			this.cacheWide = cacheWide;
		}

		public void setBeforeInvocation(boolean beforeInvocation) {
			this.beforeInvocation = beforeInvocation;
		}

		@Override
		protected StringBuilder getOperationDescription() {
			StringBuilder sb = super.getOperationDescription();
			sb.append(',');
			sb.append(this.cacheWide);
			sb.append(',');
			sb.append(this.beforeInvocation);
			return sb;
		}

		@Override
		public CacheEvictOperation build() {
			return new CacheEvictOperation(this);
		}
	}

}
