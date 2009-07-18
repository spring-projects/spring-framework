package org.springframework.ui.lifecycle;

public interface BindAndValidateLifecycleFactory {
	BindAndValidateLifecycle getLifecycle(Object model);
}
