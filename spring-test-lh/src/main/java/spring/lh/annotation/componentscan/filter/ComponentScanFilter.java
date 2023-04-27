package spring.lh.annotation.componentscan.filter;

import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;

/**
 * The type Component scan filter.
 * @author menglinghao
 */
public class ComponentScanFilter implements TypeFilter {
	@Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
		ClassMetadata classMetadata = metadataReader.getClassMetadata();

		String className = classMetadata.getClassName();

		return className.contains("componentScanConfig");
	}
}
