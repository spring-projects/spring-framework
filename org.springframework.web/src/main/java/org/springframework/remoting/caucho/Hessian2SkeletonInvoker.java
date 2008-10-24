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
import java.io.PrintWriter;

import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianDebugInputStream;
import com.caucho.hessian.io.HessianDebugOutputStream;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.SerializerFactory;
import com.caucho.hessian.server.HessianSkeleton;
import org.apache.commons.logging.Log;

import org.springframework.util.ClassUtils;
import org.springframework.util.CommonsLogWriter;

/**
 * Concrete HessianSkeletonInvoker for the Hessian 2 protocol
 * (version 3.0.20 or higher).
 *
 * @author Juergen Hoeller
 * @author Andy Piper
 * @since 2.0
 */
class Hessian2SkeletonInvoker extends HessianSkeletonInvoker {

	private static final boolean debugOutputStreamAvailable = ClassUtils.isPresent(
			"com.caucho.hessian.io.HessianDebugOutputStream", Hessian2SkeletonInvoker.class.getClassLoader());

	private final Log debugLogger;


	public Hessian2SkeletonInvoker(HessianSkeleton skeleton, SerializerFactory serializerFactory, Log debugLog) {
		super(skeleton, serializerFactory);
		this.debugLogger = debugLog;
	}

	public void invoke(final InputStream inputStream, final OutputStream outputStream) throws Throwable {
		InputStream isToUse = inputStream;
		OutputStream osToUse = outputStream;

		if (this.debugLogger != null && this.debugLogger.isDebugEnabled()) {
			PrintWriter debugWriter = new PrintWriter(new CommonsLogWriter(this.debugLogger));
			isToUse = new HessianDebugInputStream(inputStream, debugWriter);
			if (debugOutputStreamAvailable) {
				osToUse = DebugStreamFactory.createDebugOutputStream(outputStream, debugWriter);
			}
		}

		Hessian2Input in = new Hessian2Input(isToUse);
		if (this.serializerFactory != null) {
			in.setSerializerFactory(this.serializerFactory);
		}

		int code = in.read();
		if (code != 'c') {
			throw new IOException("expected 'c' in hessian input at " + code);
		}

		AbstractHessianOutput out = null;
		int major = in.read();
		int minor = in.read();
		if (major >= 2) {
			out = new Hessian2Output(osToUse);
		}
		else {
			out = new HessianOutput(osToUse);
		}
		if (this.serializerFactory != null) {
			out.setSerializerFactory(this.serializerFactory);
		}

		try {
			this.skeleton.invoke(in, out);
		}
		finally {
			try {
				in.close();
				isToUse.close();
			}
			catch (IOException ex) {
			}
			try {
				out.close();
				osToUse.close();
			}
			catch (IOException ex) {
			}
		}
	}


	/**
	 * Inner class to avoid hard dependency on Hessian 3.1.3's HessianDebugOutputStream.
	 */
	private static class DebugStreamFactory {

		public static OutputStream createDebugOutputStream(OutputStream os, PrintWriter debug) {
			return new HessianDebugOutputStream(os, debug);
		}
	}

}
