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

package org.springframework.oxm.support;

import java.io.IOException;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import org.springframework.oxm.Marshaller;
import org.springframework.util.Assert;
import org.springframework.xml.sax.AbstractXmlReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * {@link Source} implementation that uses a {@link Marshaller}.Can be constructed with a <code>Marshaller</code> and an
 * object to be marshalled.
 * <p/>
 * Even though <code>StaxSource</code> extends from <code>SAXSource</code>, calling the methods of
 * <code>SAXSource</code> is <strong>not supported</strong>. In general, the only supported operation on this class is
 * to use the <code>XMLReader</code> obtained via {@link #getXMLReader()} to parse the input source obtained via {@link
 * #getInputSource()}. Calling {@link #setXMLReader(org.xml.sax.XMLReader)} or {@link
 * #setInputSource(org.xml.sax.InputSource)} will result in <code>UnsupportedOperationException</code>s.
 *
 * @author Arjen Poutsma
 * @see javax.xml.transform.Transformer
 * @since 1.0.0
 */
public class MarshallingSource extends SAXSource {

    private final Marshaller marshaller;

    private final Object content;

    /**
     * Creates a new <code>MarshallingSource</code> with the given marshaller and content.
     *
     * @param marshaller the marshaller to use
     * @param content    the object to be marshalled
     */
    public MarshallingSource(Marshaller marshaller, Object content) {
        Assert.notNull(marshaller, "'marshaller' must not be null");
        Assert.notNull(content, "'content' must not be null");
        this.marshaller = marshaller;
        this.content = content;
        setXMLReader(new MarshallingXmlReader());
        setInputSource(new InputSource());
    }

    /** Returns the <code>Marshaller</code> used by this <code>MarshallingSource</code>. */
    public Marshaller getMarshaller() {
        return marshaller;
    }

    /** Returns the object to be marshalled. */
    public Object getContent() {
        return content;
    }

    private class MarshallingXmlReader extends AbstractXmlReader {

        public void parse(InputSource input) throws IOException, SAXException {
            parse();
        }

        public void parse(String systemId) throws IOException, SAXException {
            parse();
        }

        private void parse() throws SAXException {
            SAXResult result = new SAXResult(getContentHandler());
            result.setLexicalHandler(getLexicalHandler());
            try {
                marshaller.marshal(content, result);
            }
            catch (IOException ex) {
                SAXParseException saxException = new SAXParseException(ex.getMessage(), null, null, -1, -1, ex);
                if (getErrorHandler() != null) {
                    getErrorHandler().fatalError(saxException);
                }
                else {
                    throw saxException;
                }
            }
        }

    }
}
