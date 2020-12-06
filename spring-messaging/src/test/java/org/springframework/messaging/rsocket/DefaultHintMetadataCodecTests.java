package org.springframework.messaging.rsocket;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.codec.*;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.Assert;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
/**
 * Hint metadata test.
 **/
public class DefaultHintMetadataCodecTests {

	private DefaultHintMetadataCodec defaultHintMetadataCodec;
	private List<Decoder<?>> decoders= new ArrayList<>();
	private List<Encoder<?>> encoders= new ArrayList<>();
	private DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
	@BeforeEach
	public void setUp(){
		this.encoders.add(CharSequenceEncoder.allMimeTypes());
		this.encoders.add(new ByteBufferEncoder());
		this.encoders.add(new ByteArrayEncoder());
		this.encoders.add(new DataBufferEncoder());
		this.encoders.add(new NettyByteBufEncoder());
		this.decoders.add(StringDecoder.allMimeTypes());
		this.decoders.add(new ByteBufferDecoder());
		this.decoders.add(new ByteArrayDecoder());
		this.decoders.add(new DataBufferDecoder());
		this.decoders.add(new NettyByteBufDecoder());
		this.defaultHintMetadataCodec = new DefaultHintMetadataCodec(this.dataBufferFactory,
				new DefaultMetadataExtractor(this.decoders),this.encoders,this.decoders);
	}
	@Test
	public void testHintsCodec(){
		Map<String,Object> hints = new HashMap<>();
		hints.put("a","a_value");
		hints.put("b","b_value".getBytes());
		hints.put("c", ByteBuffer.wrap("c_value".getBytes()));
		Map<String,Object> receiveHints = new HashMap<>();
		defaultHintMetadataCodec.encodeHints(hints,e->{
			defaultHintMetadataCodec.decodeHints(e,receiveHints);
		});
		Predicate<Map<String,Object>> predicate= headers-> hints.get("a").equals(headers.get("a"))
						&& Arrays.equals((byte[]) hints.get("b"),(byte[])headers.get("b"))
						&&hints.get("c").equals(headers.get("c"));
		Assert.isTrue(predicate.test(receiveHints),"receive hints not equal with original hints");
	}

	@Test
	public void testHintsCodecEmptyValue(){
		Map<String,Object> hints = new HashMap<>();
		hints.put("a", null);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> defaultHintMetadataCodec.encodeHints(hints,e->{}))
				.withMessage("'value' must not be null");
	}

	@Test
	public void testHintsCodecWithOutEncoder(){
		Map<String,Object> hints = new HashMap<>();
		hints.put("a", Unpooled.wrappedBuffer(ByteBuffer.wrap("a_value".getBytes())));
		DefaultHintMetadataCodec defaultHintMetadataCodec = new DefaultHintMetadataCodec(this.dataBufferFactory,
				new DefaultMetadataExtractor(this.decoders),this.encoders.subList(0,4),this.decoders);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> defaultHintMetadataCodec.encodeHints(hints,e->{}))
				.withMessage("No encoder for io.netty.buffer.UnpooledHeapByteBuf");
	}

}
