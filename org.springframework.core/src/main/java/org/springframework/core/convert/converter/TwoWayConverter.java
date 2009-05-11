package org.springframework.core.convert.converter;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConvertException;

/**
 * A converter that can also convert a target object of type T to a source of class S.
 * <p>
 * Implementations of this interface are thread-safe and can be shared. Converters are typically registered with and
 * accessed through a {@link ConversionService}.
 * </p>
 * @author Keith Donald
 */
public interface TwoWayConverter<S, T> extends Converter<S, T> {

	/**
	 * Convert the target of type T back to source type S.
	 * @param target the target object to convert, which must be an instance of T
	 * @return the converted object, which must be an instance of S
	 * @throws Exception an exception occurred performing the conversion; may be any checked exception, the conversion
	 * system will handle wrapping the failure in a {@link ConvertException} that provides a consistent type
	 * conversion error context
	 */
	public S convertBack(T target) throws Exception;
}
