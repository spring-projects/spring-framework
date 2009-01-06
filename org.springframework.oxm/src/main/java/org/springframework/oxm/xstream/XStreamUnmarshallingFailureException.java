/*
 * Copyright 2006 the original author or authors.
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

package org.springframework.oxm.xstream;

import javax.xml.stream.XMLStreamException;

import com.thoughtworks.xstream.alias.CannotResolveClassException;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.StreamException;
import org.springframework.oxm.UnmarshallingFailureException;

/**
 * XStream-specific subclass of <code>UnmarshallingFailureException</code>.
 *
 * @author Arjen Poutsma
 * @since 1.0.0
 */
public class XStreamUnmarshallingFailureException extends UnmarshallingFailureException {

    public XStreamUnmarshallingFailureException(StreamException ex) {
        super("XStream unmarshalling exception: " + ex.getMessage(), ex);
    }

    public XStreamUnmarshallingFailureException(CannotResolveClassException ex) {
        super("XStream resolving exception: " + ex.getMessage(), ex);
    }

    public XStreamUnmarshallingFailureException(ConversionException ex) {
        super("XStream conversion exception: " + ex.getMessage(), ex);
    }

    public XStreamUnmarshallingFailureException(String msg) {
        super(msg);
    }

    public XStreamUnmarshallingFailureException(String msg, XMLStreamException ex) {
        super(msg, ex);
    }

}
