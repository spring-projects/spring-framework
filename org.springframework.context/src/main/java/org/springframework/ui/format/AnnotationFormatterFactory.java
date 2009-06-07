package org.springframework.ui.format;

import java.lang.annotation.Annotation;

public interface AnnotationFormatterFactory<A extends Annotation, T> {
	Formatter<T> getFormatter(A annotation);	
}
