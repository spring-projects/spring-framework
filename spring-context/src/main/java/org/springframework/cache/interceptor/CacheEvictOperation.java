/*
 * Copyright 2002-2013 the original author or authors.
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

/**
 * Class describing a cache 'evict' operation.
 *
 * @author Costin Leau
 * @since 3.1
 */
public class CacheEvictOperation extends CacheOperation {

    private final boolean cacheWide;

    private final boolean beforeInvocation;

    public boolean isCacheWide() {
        return this.cacheWide;
    }

    public boolean isBeforeInvocation() {
        return this.beforeInvocation;
    }

    public CacheEvictOperation(CacheEvictOperation.Builder b) {
        super(b);
        this.cacheWide = b.cacheWide;
        this.beforeInvocation = b.beforeInvocation;
    }

    @Override
    protected StringBuilder getOperationDescription() {
        StringBuilder sb = super.getOperationDescription();
        sb.append(",");
        sb.append(this.cacheWide);
        sb.append(",");
        sb.append(this.beforeInvocation);
        return sb;
    }

    public static class Builder extends CacheOperation.Builder {
        private boolean cacheWide = false;

        private boolean beforeInvocation = false;

        public void setCacheWide(boolean cacheWide) {
            this.cacheWide = cacheWide;
        }

        public void setBeforeInvocation(boolean beforeInvocation) {
            this.beforeInvocation = beforeInvocation;
        }

        public CacheEvictOperation build() {
	        return new CacheEvictOperation(this);
        }
    }
}
