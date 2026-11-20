/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.oxm.jaxb;

import jakarta.activation.DataHandler;
import jakarta.xml.bind.annotation.XmlAttachmentRef;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(namespace = "http://springframework.org/spring-ws")
public class BinaryObject {

	@XmlElement(namespace = "http://springframework.org/spring-ws")
	private byte[] bytes;

	@XmlElement(namespace = "http://springframework.org/spring-ws")
	private DataHandler dataHandler;

	@XmlElement(namespace = "http://springframework.org/spring-ws")
	@XmlAttachmentRef
	private DataHandler swaDataHandler;

	public BinaryObject() {
	}

	public BinaryObject(byte[] bytes, DataHandler dataHandler) {
		this.bytes = bytes;
		this.dataHandler = dataHandler;
		swaDataHandler = dataHandler;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public DataHandler getDataHandler() {
		return dataHandler;
	}

	public DataHandler getSwaDataHandler() {
		return swaDataHandler;
	}
}
