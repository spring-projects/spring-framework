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

import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;

import junit.framework.TestCase;

public class XStreamUtilsTest extends TestCase {

    public void testConvertStreamException() {
        assertTrue("Invalid exception conversion", XStreamUtils.convertXStreamException(
                new StreamException(new Exception()), true) instanceof XStreamMarshallingFailureException);
        assertTrue("Invalid exception conversion", XStreamUtils.convertXStreamException(
                new StreamException(new Exception()), false) instanceof XStreamUnmarshallingFailureException);
    }

    public void testConvertCannotResolveClassException() {
        assertTrue("Invalid exception conversion", XStreamUtils.convertXStreamException(
                new CannotResolveClassException(""), true) instanceof XStreamMarshallingFailureException);
        assertTrue("Invalid exception conversion", XStreamUtils.convertXStreamException(
                new CannotResolveClassException(""), false) instanceof XStreamUnmarshallingFailureException);
    }
}