/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.remoting.jaxrpc;

import javax.xml.namespace.QName;
import javax.xml.rpc.soap.SOAPFaultException;

import org.springframework.remoting.soap.SoapFaultException;

/**
 * Spring SoapFaultException adapter for the JAX-RPC
 * {@link javax.xml.rpc.soap.SOAPFaultException} class.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class JaxRpcSoapFaultException extends SoapFaultException {

	/**
	 * Constructor for JaxRpcSoapFaultException.
	 * @param original the original JAX-RPC SOAPFaultException to wrap
	 */
	public JaxRpcSoapFaultException(SOAPFaultException original) {
		super(original.getMessage(), original);
	}

	/**
	 * Return the wrapped JAX-RPC SOAPFaultException.
	 */
	public final SOAPFaultException getOriginalException() {
		return (SOAPFaultException) getCause();
	}


	public String getFaultCode() {
		return getOriginalException().getFaultCode().toString();
	}

	public QName getFaultCodeAsQName() {
		return getOriginalException().getFaultCode();
	}

	public String getFaultString() {
		return getOriginalException().getFaultString();
	}

	public String getFaultActor() {
		return getOriginalException().getFaultActor();
	}

}
