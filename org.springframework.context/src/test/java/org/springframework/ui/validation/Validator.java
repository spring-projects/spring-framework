package org.springframework.ui.validation;

import java.util.List;

public interface Validator {

	void validate(Object model, List<String> properties);

}
