package org.springframework.ui.format.number;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A annotation to apply to a BigDecimal property to have property values formatted as currency using a {@link CurrencyFormatter}.
 * @author Keith Donald
 */
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrencyFormat {

}
