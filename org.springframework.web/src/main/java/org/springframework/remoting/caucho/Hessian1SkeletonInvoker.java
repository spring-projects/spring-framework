/*
 * Copyright 2002-2008 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.SerializerFactory;
import com.caucho.hessian.server.HessianSkeleton;

import org.springframework.util.ClassUtils;

/**
 * Concrete HessianSkeletonInvoker for the Hessian 1 protocol
 * (version 3.0.19 or lower).
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
class Hessian1SkeletonInvoker extends HessianSkeletonInvoker {

	private static final Method invokeMethod;

	private static final boolean applySerializerFactoryToOutput;

	static {
		invokeMethod = ClassUtils.getMethodIfAvailable(
				HessianSkeleton.class, "invoke", new Class[] {HessianInput.class, HessianOutput.class});
		applySerializerFactoryToOutput =
				ClassUtils.hasMethod(HessianOutput.class, "setSerializerFactory", new Class[] {SerializerFactory.class});
	}


	public Hessian1SkeletonInvoker(HessianSkeleton skeleton, SerializerFactory serializerFactory) {
		super(skeleton, serializerFactory);
		if (invokeMethod == null) {
			throw new IllegalStateException("Hessian 1 (version 3.0.19-) not present");
		}
	}

	public void invoke(InputStream inputStream, OutputStream outputStream) throws Throwable {
		HessianInput in = new HessianInput(inputStream);
		HessianOutput out = new HessianOutput(outputStream);
		if (this.serializerFactory != null) {
			in.setSerializerFactory(this.serializerFactory);
			if (applySerializerFactoryToOutput) {
				out.setSerializerFactory(this.serializerFactory);
			}
		}
		try {
			invokeMethod.invoke(this.skeleton, new Object[] {in, out});
		}
		finally {
			try {
				in.close();
				inputStream.close();
			}
			catch (IOException ex) {
			}
			try {
				out.close();
				outputStream.close();
			}
			catch (IOException ex) {
			}
		}

	}

}
