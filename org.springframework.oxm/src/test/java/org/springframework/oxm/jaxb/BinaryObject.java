/*
 * Copyright 2007 the original author or authors.
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

package org.springframework.oxm.jaxb;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
