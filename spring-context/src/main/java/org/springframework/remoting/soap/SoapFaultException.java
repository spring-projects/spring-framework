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

package org.springframework.remoting.soap;

import javax.xml.namespace.QName;

import org.springframework.remoting.RemoteInvocationFailureException;

/**
 * RemoteInvocationFailureException subclass that provides the details
 * of a SOAP fault.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see javax.xml.rpc.soap.SOAPFaultException
 * @see javax.xml.ws.soap.SOAPFaultException
 */
public abstract class SoapFaultException extends RemoteInvocationFailureException {

	/**
	 * Constructor for SoapFaultException.
	 * @param msg the detail message
	 * @param cause the root cause from the SOAP API in use
	 */
	protected SoapFaultException(String msg, Throwable cause) {
		super(msg, cause);
	}


	/**
	 * Return the SOAP fault code.
	 */
	public abstract String getFaultCode();

	/**
	 * Return the SOAP fault code as a {@code QName} object.
	 */
	public abstract QName getFaultCodeAsQName();

	/**
	 * Return the descriptive SOAP fault string.
	 */
	public abstract String getFaultString();

	/**
	 * Return the actor that caused this fault.
	 */
	public abstract String getFaultActor();

}
