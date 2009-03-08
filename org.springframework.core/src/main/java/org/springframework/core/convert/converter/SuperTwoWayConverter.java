package org.springframework.core.convert.converter;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;

/**
 * A super converter that can also convert a target object of type T to a source of class hierarchy S.
 * <p>
 * Implementations of this interface are thread-safe and can be shared. Converters are typically registered with and
 * accessed through a {@link ConversionService}.
 * </p>
 * @author Keith Donald
 */
public interface SuperTwoWayConverter<S, T> extends SuperConverter<S, T> {

	/**
	 * Convert the target of type T to an instance of S.
	 * @param target the target object to convert, whose class must be equal to or a subclass of T
	 * @param sourceClass the requested source class to convert to, which must be equal to S or extend from S
	 * @return the converted object, which must be an instance of S
	 * @throws Exception an exception occurred performing the conversion; may be any checked exception, the conversion
	 * system will handle wrapping the failure in a {@link ConversionException} that provides a consistent type
	 * conversion error context
	 */
	public <RS extends S> RS convertBack(T target, Class<RS> sourceClass) throws Exception;

}