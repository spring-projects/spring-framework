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

package org.springframework.remoting.jaxws;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;

import org.springframework.remoting.soap.SoapFaultException;

/**
 * Spring SoapFaultException adapter for the JAX-WS
 * {@link javax.xml.ws.soap.SOAPFaultException} class.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
@SuppressWarnings("serial")
public class JaxWsSoapFaultException extends SoapFaultException {

	/**
	 * Constructor for JaxWsSoapFaultException.
	 * @param original the original JAX-WS SOAPFaultException to wrap
	 */
	public JaxWsSoapFaultException(SOAPFaultException original) {
		super(original.getMessage(), original);
	}

	/**
	 * Return the wrapped JAX-WS SOAPFault.
	 */
	public final SOAPFault getFault() {
		return ((SOAPFaultException) getCause()).getFault();
	}


	@Override
	public String getFaultCode() {
		return getFault().getFaultCode();
	}

	@Override
	public QName getFaultCodeAsQName() {
		return getFault().getFaultCodeAsQName();
	}

	@Override
	public String getFaultString() {
		return getFault().getFaultString();
	}

	@Override
	public String getFaultActor() {
		return getFault().getFaultActor();
	}

}
