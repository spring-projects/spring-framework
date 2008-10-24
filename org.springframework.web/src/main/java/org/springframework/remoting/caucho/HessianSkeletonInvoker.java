/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.remoting.caucho;

import java.io.InputStream;
import java.io.OutputStream;

import com.caucho.hessian.server.HessianSkeleton;
import com.caucho.hessian.io.SerializerFactory;

import org.springframework.util.Assert;

/**
 * Internal invoker strategy for a Hessian skeleton.
 * Allows for common handling of Hessian protocol version 1 and 2.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
abstract class HessianSkeletonInvoker {

	/**
	 * Wrapped HessianSkeleton, available to subclasses.
	 */
	protected final HessianSkeleton skeleton;

	/**
	 * Hessian SerializerFactory (if any), available to subclasses.
	 */
	protected final SerializerFactory serializerFactory;


	/**
	 * Create a new HessianSkeletonInvoker for the given skeleton.
	 * @param skeleton the HessianSkeleton to wrap
	 * @param serializerFactory the Hessian SerializerFactory to use, if any
	 */
	public HessianSkeletonInvoker(HessianSkeleton skeleton, SerializerFactory serializerFactory) {
		Assert.notNull(skeleton, "HessianSkeleton must not be null");
		this.skeleton = skeleton;
		this.serializerFactory = serializerFactory;
	}


	/**
	 * Invoke the given skeleton based on the given input/output streams.
	 * @param inputStream the stream containing the Hessian input
	 * @param outputStream the stream to receive the Hessian output
	 * @throws Throwable if the skeleton invocation failed
	 */
	public abstract void invoke(InputStream inputStream, OutputStream outputStream) throws Throwable;

}
