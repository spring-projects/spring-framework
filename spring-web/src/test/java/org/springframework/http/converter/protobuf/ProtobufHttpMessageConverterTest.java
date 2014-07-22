package org.springframework.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.protobuf.Msg;
import org.springframework.protobuf.SecondMsg;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by aantonov on 7/1/14.
 */
public class ProtobufHttpMessageConverterTest {
    private ProtobufHttpMessageConverter converter;
    private Msg testMsg;

    @Before
    public void setUp() {
        converter = new ProtobufHttpMessageConverter(new ExtensionRegistryInitializer() {
            @Override
            public void initializeExtensionRegistry(ExtensionRegistry registry) {
                // DO Nothing
            }
        });
        testMsg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();
    }

    @Test
    public void canRead() {
        assertTrue("Message not supported", converter.canRead(Msg.class, null));
        assertTrue("Message not supported", converter.canRead(Msg.class, ProtobufHttpMessageConverter.PROTOBUF));
    }

    @Test
    public void canWrite() {
        assertTrue("Message not supported", converter.canWrite(Msg.class, null));
        assertTrue("Message not supported", converter.canWrite(Msg.class, ProtobufHttpMessageConverter.PROTOBUF));
    }

    @Test
    public void read() throws IOException {
        byte[] body = testMsg.toByteArray();
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
        inputMessage.getHeaders().setContentType(ProtobufHttpMessageConverter.PROTOBUF);
        Message result = converter.read(Msg.class, inputMessage);
        assertEquals("Invalid data", testMsg, result);
    }

    @Test
    public void write() throws IOException {
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        MediaType contentType = ProtobufHttpMessageConverter.PROTOBUF;
        converter.write(testMsg, contentType, outputMessage);
        assertEquals("Invalid content type", contentType, outputMessage.getHeaders().getContentType());
        assertTrue("Invalid size", outputMessage.getBodyAsBytes().length > 0);
        Message result = Msg.parseFrom(outputMessage.getBodyAsBytes());
        assertEquals("Invalid data", testMsg, result);
    }
}
