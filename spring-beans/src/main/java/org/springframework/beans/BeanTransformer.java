package org.springframework.beans;

import org.springframework.util.Assert;

/**
 * Methods for populating Mutable, Immutable and Hybrid JavaBeans properties via reflection.
 * @author borriello.fabio
 */
public class BeanTransformer {
	/**
	 * Transforms the given source bean into the given target bean class copying all its properties.
	 * <p>Note: The source and target classes have to match or even be derived.
	 * <p>This is just a convenience method. For more complex transfer needs,
	 * consider using a {@link com.hotels.beans.transformer.BeanTransformer} and its settings.
	 * @see <a href="https://hotelsdotcom.github.io/bull/apidocs">
	 * https://hotelsdotcom.github.io/bull/apidocs</a>
	 * @param source the source bean
	 * @param targetClass the target bean class
	 * @param <K> the target object type
	 * @return a transformed copy of the source object into the destination object
	 * @throws BeansException if the transformation failed
	 * @see BeanWrapper
	 */
	public <K> K transform(final Object source, final Class<K> targetClass) throws BeansException {
		return new com.hotels.beans.BeanUtils().getTransformer()
				.setDefaultValueForMissingField(true)
				.transform(source, targetClass);
	}

	/**
	 * Transforms the given source bean into the given target bean class copying all its properties through the given
	 * {@link com.hotels.beans.transformer.BeanTransformer} class.
	 * <p>Note: The source and target classes have to match or even be derived.
	 * <p>This is just a convenience method. For more complex transfer needs,
	 * consider using a {@link com.hotels.beans.transformer.BeanTransformer} and its settings.
	 * @see <a href="https://hotelsdotcom.github.io/bull/apidocs">
	 * https://hotelsdotcom.github.io/bull/apidocs</a>
	 * @param source the source bean
	 * @param targetClass the target bean class
	 * @param beanTransformer the bean transformer to use for the transformation. {@link com.hotels.beans.transformer.BeanTransformer}
	 * @param <K> the target object type
	 * @return a transformed copy of the source object into the destination object
	 * @throws BeansException if the transformation failed
	 * @see BeanWrapper
	 */
	public <K> K transform(final Object source, final Class<K> targetClass, final com.hotels.beans.transformer.BeanTransformer beanTransformer)
			throws BeansException {
		Assert.notNull(beanTransformer, "BeanTransformer must not be null");
		return beanTransformer.transform(source, targetClass);
	}

	/**
	 * Returns a bean transformer able to transform properties of any type of java bean.
	 * <p>Note: The {@link com.hotels.beans.transformer.BeanTransformer} can be configured in order to apply any type of transformation on a java bean.
	 * @see <a href="https://hotelsdotcom.github.io/bull/apidocs/com/hotels/beans/transformer/BeanTransformer.html">
	 * https://hotelsdotcom.github.io/bull/apidocs/com/hotels/beans/transformer/BeanTransformer.html</a>
	 * @return BeanTransformer instance
	 * @see com.hotels.beans.transformer.BeanTransformer
	 */
	public static com.hotels.beans.transformer.BeanTransformer getBeanTransformer() {
		return new com.hotels.beans.BeanUtils().getTransformer();
	}
}
