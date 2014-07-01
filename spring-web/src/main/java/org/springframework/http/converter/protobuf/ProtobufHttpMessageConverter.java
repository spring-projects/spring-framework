package org.springframework.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.googlecode.protobuf.format.HtmlFormat;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.XmlFormat;
import org.apache.log4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.FileCopyUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author  Alex Antonov
 */
public class ProtobufHttpMessageConverter extends AbstractHttpMessageConverter<Message> {
    private static Logger log = Logger.getLogger(ProtobufHttpMessageConverter.class);

    public static String LS = System.getProperty("line.separator");
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final MediaType PROTOBUF = new MediaType("application", "x-protobuf", DEFAULT_CHARSET);
    public static final MediaType XML = new MediaType("application", "xml", DEFAULT_CHARSET);
    public static final MediaType JSON = new MediaType("application", "json", DEFAULT_CHARSET);
    public static final MediaType TEXT = new MediaType("text", "plain");
    public static final MediaType HTML = new MediaType("text", "html");
    public static final String X_PROTOBUF_SCHEMA_HEADER = "X-Protobuf-Schema";
    public static final String X_PROTOBUF_MESSAGE_HEADER = "X-Protobuf-Message";

    private ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();

    private static final ConcurrentHashMap<Class<?>, Method> newBuilderMethodCache =
            new ConcurrentHashMap<Class<?>, Method>();

    public ProtobufHttpMessageConverter() {
        this(null);
    }

    public ProtobufHttpMessageConverter(ExtensionRegistryInitializer registryInitializer) {
        super(PROTOBUF, HTML, TEXT, XML, JSON);
        initializeExtentionRegistry(registryInitializer);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Message.class.isAssignableFrom(clazz);
    }

    @Override
    protected Message readInternal(Class<? extends Message> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        MediaType contentType = inputMessage.getHeaders().getContentType();
        contentType = contentType != null ? contentType : PROTOBUF;
        try {
            Method m = getNewBuilderMessageMethod(clazz);
            Message.Builder builder = (Message.Builder) m.invoke(clazz);
            if (isJson(contentType)) {
                String data = convertInputStreamToString(inputMessage.getBody());
                JsonFormat.merge(data, extensionRegistry, builder);
            } else if (isText(contentType)) {
                String data = convertInputStreamToString(inputMessage.getBody());
                TextFormat.merge(data, extensionRegistry, builder);
            } else if (isXml(contentType)) {
                String data = convertInputStreamToString(inputMessage.getBody());
                XmlFormat.merge(data, extensionRegistry, builder);
            } else {
                InputStream is = inputMessage.getBody();
                builder.mergeFrom(is, extensionRegistry);
            }
            return builder.build();
        } catch (Exception e) {
            throw new HttpMessageNotReadableException("Unable to convert inputMessage to Proto object", e);
        }
    }

    @Override
    protected void writeInternal(Message message, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        List<MediaType> contentTypes = outputMessage.getHeaders().getAccept();

        for(MediaType contentType : contentTypes) {
            Charset charset = contentType.getCharSet() != null ? contentType.getCharSet() : DEFAULT_CHARSET;

            if (isHtml(contentType)) {
                String data = HtmlFormat.printToString(message);
                FileCopyUtils.copy(data.getBytes(charset.name()), outputMessage.getBody());
            } else if (isJson(contentType)) {
                String data = JsonFormat.printToString(message);
                FileCopyUtils.copy(data.getBytes(charset.name()), outputMessage.getBody());
            } else if (isText(contentType)) {
                String data = TextFormat.printToString(message);
                FileCopyUtils.copy(data.getBytes(charset.name()), outputMessage.getBody());
            } else if (isXml(contentType)) {
                String data = XmlFormat.printToString(message);
                FileCopyUtils.copy(data.getBytes(charset.name()), outputMessage.getBody());
            } else {
                setProtoHeader(outputMessage, message);
                FileCopyUtils.copy(message.toByteArray(), outputMessage.getBody());
            }

            return; //We don't want to loop through all Accept values, since we don't really expect multiple that will all match.
        }
    }

    @Override
    protected MediaType getDefaultContentType(Message message) {
        return PROTOBUF;
    }

    @Override
    protected Long getContentLength(Message message, MediaType contentType) {
        Charset charset = contentType.getCharSet() != null ? contentType.getCharSet() : DEFAULT_CHARSET;

        try {
            if (isHtml(contentType)) {
                String data = HtmlFormat.printToString(message);
                return (long) data.getBytes(charset.name()).length;
            } else if (isJson(contentType)) {
                String data = JsonFormat.printToString(message);
                return (long) data.getBytes(charset.name()).length;
            } else if (isText(contentType)) {
                String data = TextFormat.printToString(message);
                return (long) data.getBytes(charset.name()).length;
            } else if (isXml(contentType)) {
                String data = XmlFormat.printToString(message);
                return (long) data.getBytes(charset.name()).length;
            } else {
                return (long) message.toByteArray().length;
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to calculate lenth due to UnsupportedEncodingException", e);
        }
    }

    protected boolean isJson(MediaType contentType) {
        return JSON.getType().equals(contentType.getType()) && JSON.getSubtype().equals(contentType.getSubtype());
    }

    protected boolean isText(MediaType contentType) {
        return TEXT.getType().equals(contentType.getType()) && TEXT.getSubtype().equals(contentType.getSubtype());
    }

    protected boolean isXml(MediaType contentType) {
        return XML.getType().equals(contentType.getType()) && XML.getSubtype().equals(contentType.getSubtype());
    }

    protected boolean isHtml(MediaType contentType) {
        return HTML.getType().equals(contentType.getType()) && HTML.getSubtype().equals(contentType.getSubtype());
    }

    private Method getNewBuilderMessageMethod(Class<? extends Message> clazz) throws NoSuchMethodException {
        Method m = newBuilderMethodCache.get(clazz);
        if (m == null) {
            m = clazz.getMethod("newBuilder");
            newBuilderMethodCache.put(clazz, m);
        }
        return m;
    }

    private void initializeExtentionRegistry(ExtensionRegistryInitializer registryInitializer) {
        if (registryInitializer != null) {
            registryInitializer.initializeExtensionRegistry(extensionRegistry);
        }
    }

    private String convertInputStreamToString(InputStream io) {
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(io));
            String line = reader.readLine();
            while (line != null) {
                sb.append(line).append(LS);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to obtain an InputStream", e);

        }
        return sb.toString();
    }

    private void setProtoHeader(final HttpOutputMessage response, final Message message) {
        String protoFileName;
        String protoMessageName;
        try {
            protoFileName = message.getDescriptorForType().getFile().getName();
            response.getHeaders().set(X_PROTOBUF_SCHEMA_HEADER, protoFileName);

            protoMessageName = message.getDescriptorForType().getFullName();
            response.getHeaders().set(X_PROTOBUF_MESSAGE_HEADER, protoMessageName);
        } catch (NullPointerException e) {
            // Shouldn't happen in running environment
            // Catching and ignoring it doesn't damage the response
            log.error("Exception caught while trying to set the ProtoHeader", e);
        }
    }
}
