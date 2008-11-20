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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.support.RemoteExporter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CommonsLogWriter;

/**
 * General stream-based protocol exporter for a Hessian endpoint.
 *
 * <p>Hessian is a slim, binary RPC protocol.
 * For information on Hessian, see the
 * <a href="http://www.caucho.com/hessian">Hessian website</a>.
 * This exporter requires Hessian 3.0.20 or above.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @see #invoke(java.io.InputStream, java.io.OutputStream)
 * @see HessianServiceExporter
 * @see SimpleHessianServiceExporter
 */
public class HessianExporter extends RemoteExporter implements InitializingBean {

	private static final boolean debugOutputStreamAvailable = ClassUtils.isPresent(
			"com.caucho.hessian.io.HessianDebugOutputStream", HessianExporter.class.getClassLoader());

	private SerializerFactory serializerFactory = new SerializerFactory();

	private Log debugLogger;

	private HessianSkeleton skeleton;


	/**
	 * Specify the Hessian SerializerFactory to use.
	 * <p>This will typically be passed in as an inner bean definition
	 * of type <code>com.caucho.hessian.io.SerializerFactory</code>,
	 * with custom bean property values applied.
	 */
	public void setSerializerFactory(SerializerFactory serializerFactory) {
		this.serializerFactory = (serializerFactory != null ? serializerFactory : new SerializerFactory());
	}

	/**
	 * Set whether to send the Java collection type for each serialized
	 * collection. Default is "true".
	 */
	public void setSendCollectionType(boolean sendCollectionType) {
		this.serializerFactory.setSendCollectionType(sendCollectionType);
	}

	/**
	 * Set whether Hessian's debug mode should be enabled, logging to
	 * this exporter's Commons Logging log. Default is "false".
	 * @see com.caucho.hessian.client.HessianProxyFactory#setDebug
	 */
	public void setDebug(boolean debug) {
		this.debugLogger = (debug ? logger : null);
	}


	public void afterPropertiesSet() {
		prepare();
	}

	/**
	 * Initialize this exporter.
	 */
	public void prepare() {
		checkService();
		checkServiceInterface();
		this.skeleton = new HessianSkeleton(getProxyForService(), getServiceInterface());
	}


	/**
	 * Perform an invocation on the exported object.
	 * @param inputStream the request stream
	 * @param outputStream the response stream
	 * @throws Throwable if invocation failed
	 */
	public void invoke(InputStream inputStream, OutputStream outputStream) throws Throwable {
		Assert.notNull(this.skeleton, "Hessian exporter has not been initialized");
		ClassLoader originalClassLoader = overrideThreadContextClassLoader();
		try {
			doInvoke(inputStream, outputStream);
		}
		finally {
			resetThreadContextClassLoader(originalClassLoader);
		}
	}

	public void doInvoke(final InputStream inputStream, final OutputStream outputStream) throws Throwable {
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
				// ignore
			}
			try {
				out.close();
				osToUse.close();
			}
			catch (IOException ex) {
				// ignore
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
