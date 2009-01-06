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

import com.thoughtworks.xstream.alias.CannotResolveClassException;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.StreamException;
import org.springframework.oxm.MarshallingFailureException;

/**
 * XStream-specific subclass of <code>MarshallingFailureException</code>.
 *
 * @author Arjen Poutsma
 * @since 1.0.0
 */
public class XStreamMarshallingFailureException extends MarshallingFailureException {

    public XStreamMarshallingFailureException(String msg) {
        super(msg);
    }

    public XStreamMarshallingFailureException(StreamException ex) {
        super("XStream marshalling exception: " + ex.getMessage(), ex);

    }

    public XStreamMarshallingFailureException(CannotResolveClassException ex) {
        super("XStream resolving exception: " + ex.getMessage(), ex);
    }

    public XStreamMarshallingFailureException(ConversionException ex) {
        super("XStream conversion exception: " + ex.getMessage(), ex);
    }

}
