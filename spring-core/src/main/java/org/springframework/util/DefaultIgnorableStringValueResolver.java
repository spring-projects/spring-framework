package org.springframework.util;

import org.springframework.lang.Nullable;

public interface DefaultIgnorableStringValueResolver extends StringValueResolver {

	@Nullable
	String resolveStringValueIgnoringDefault(String strVal);
}
