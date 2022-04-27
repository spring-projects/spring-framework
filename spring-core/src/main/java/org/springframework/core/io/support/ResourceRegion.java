/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.io.support;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Region of a {@link Resource} implementation, materialized by a {@code position}
 * within the {@link Resource} and a byte {@code count} for the length of that region.
 *
 * @author Arjen Poutsma
 * @since 4.3
 */
public class ResourceRegion {

	private final Resource resource;

	private final long position;

	private final long count;


	/**
	 * Create a new {@code ResourceRegion} from a given {@link Resource}.
	 * This region of a resource is represented by a start {@code position}
	 * and a byte {@code count} within the given {@code Resource}.
	 * @param resource a Resource
	 * @param position the start position of the region in that resource
	 * @param count the byte count of the region in that resource
	 */
	public ResourceRegion(Resource resource, long position, long count) {
		Assert.notNull(resource, "Resource must not be null");
		Assert.isTrue(position >= 0, "'position' must be larger than or equal to 0");
		Assert.isTrue(count >= 0, "'count' must be larger than or equal to 0");
		this.resource = resource;
		this.position = position;
		this.count = count;
	}


	/**
	 * Return the underlying {@link Resource} for this {@code ResourceRegion}.
	 */
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * Return the start position of this region in the underlying {@link Resource}.
	 */
	public long getPosition() {
		return this.position;
	}

	/**
	 * Return the byte count of this region in the underlying {@link Resource}.
	 */
	public long getCount() {
		return this.count;
	}

}
