/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jpa.persistenceunit;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.context.testfixture.jndi.SimpleNamingContextBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit and integration tests for the JPA XML resource parsing support.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Nicholas Williams
 */
public class PersistenceXmlParsingTests {

	@Test
	public void testMetaInfCase() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/META-INF/persistence.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertThat(info).isNotNull();
		assertThat(info.length).isEqualTo(1);
		assertThat(info[0].getPersistenceUnitName()).isEqualTo("OrderManagement");

		assertThat(info[0].getJarFileUrls().size()).isEqualTo(2);
		assertThat(info[0].getJarFileUrls().get(0)).isEqualTo(new ClassPathResource("order.jar").getURL());
		assertThat(info[0].getJarFileUrls().get(1)).isEqualTo(new ClassPathResource("order-supplemental.jar").getURL());

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();
	}

	@Test
	public void testExample1() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-example1.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertThat(info).isNotNull();
		assertThat(info.length).isEqualTo(1);
		assertThat(info[0].getPersistenceUnitName()).isEqualTo("OrderManagement");

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();
	}

	@Test
	public void testExample2() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-example2.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertThat(info).isNotNull();
		assertThat(info.length).isEqualTo(1);

		assertThat(info[0].getPersistenceUnitName()).isEqualTo("OrderManagement2");

		assertThat(info[0].getMappingFileNames().size()).isEqualTo(1);
		assertThat(info[0].getMappingFileNames().get(0)).isEqualTo("mappings.xml");
		assertThat(info[0].getProperties().keySet().size()).isEqualTo(0);

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();
	}

	@Test
	public void testExample3() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-example3.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertThat(info).isNotNull();
		assertThat(info.length).isEqualTo(1);
		assertThat(info[0].getPersistenceUnitName()).isEqualTo("OrderManagement3");

		assertThat(info[0].getJarFileUrls().size()).isEqualTo(2);
		assertThat(info[0].getJarFileUrls().get(0)).isEqualTo(new ClassPathResource("order.jar").getURL());
		assertThat(info[0].getJarFileUrls().get(1)).isEqualTo(new ClassPathResource("order-supplemental.jar").getURL());

		assertThat(info[0].getProperties().keySet().size()).isEqualTo(0);
		assertThat(info[0].getJtaDataSource()).isNull();
		assertThat(info[0].getNonJtaDataSource()).isNull();

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();
	}

	@Test
	public void testExample4() throws Exception {
		SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
		DataSource ds = new DriverManagerDataSource();
		builder.bind("java:comp/env/jdbc/MyDB", ds);

		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-example4.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertThat(info).isNotNull();
		assertThat(info.length).isEqualTo(1);
		assertThat(info[0].getPersistenceUnitName()).isEqualTo("OrderManagement4");

		assertThat(info[0].getMappingFileNames().size()).isEqualTo(1);
		assertThat(info[0].getMappingFileNames().get(0)).isEqualTo("order-mappings.xml");

		assertThat(info[0].getManagedClassNames().size()).isEqualTo(3);
		assertThat(info[0].getManagedClassNames().get(0)).isEqualTo("com.acme.Order");
		assertThat(info[0].getManagedClassNames().get(1)).isEqualTo("com.acme.Customer");
		assertThat(info[0].getManagedClassNames().get(2)).isEqualTo("com.acme.Item");

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should be true when no value.").isTrue();

		assertThat(info[0].getTransactionType()).isSameAs(PersistenceUnitTransactionType.RESOURCE_LOCAL);
		assertThat(info[0].getProperties().keySet().size()).isEqualTo(0);

		builder.clear();
	}

	@Test
	public void testExample5() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-example5.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertThat(info).isNotNull();
		assertThat(info.length).isEqualTo(1);
		assertThat(info[0].getPersistenceUnitName()).isEqualTo("OrderManagement5");

		assertThat(info[0].getMappingFileNames().size()).isEqualTo(2);
		assertThat(info[0].getMappingFileNames().get(0)).isEqualTo("order1.xml");
		assertThat(info[0].getMappingFileNames().get(1)).isEqualTo("order2.xml");

		assertThat(info[0].getJarFileUrls().size()).isEqualTo(2);
		assertThat(info[0].getJarFileUrls().get(0)).isEqualTo(new ClassPathResource("order.jar").getURL());
		assertThat(info[0].getJarFileUrls().get(1)).isEqualTo(new ClassPathResource("order-supplemental.jar").getURL());

		assertThat(info[0].getPersistenceProviderClassName()).isEqualTo("com.acme.AcmePersistence");
		assertThat(info[0].getProperties().keySet().size()).isEqualTo(0);

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();
	}

	@Test
	public void testExampleComplex() throws Exception {
		DataSource ds = new DriverManagerDataSource();

		String resource = "/org/springframework/orm/jpa/persistence-complex.xml";
		MapDataSourceLookup dataSourceLookup = new MapDataSourceLookup();
		Map<String, DataSource> dataSources = new HashMap<>();
		dataSources.put("jdbc/MyPartDB", ds);
		dataSources.put("jdbc/MyDB", ds);
		dataSourceLookup.setDataSources(dataSources);
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), dataSourceLookup);
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertThat(info.length).isEqualTo(2);

		PersistenceUnitInfo pu1 = info[0];

		assertThat(pu1.getPersistenceUnitName()).isEqualTo("pu1");

		assertThat(pu1.getPersistenceProviderClassName()).isEqualTo("com.acme.AcmePersistence");

		assertThat(pu1.getMappingFileNames().size()).isEqualTo(1);
		assertThat(pu1.getMappingFileNames().get(0)).isEqualTo("ormap2.xml");

		assertThat(pu1.getJarFileUrls().size()).isEqualTo(1);
		assertThat(pu1.getJarFileUrls().get(0)).isEqualTo(new ClassPathResource("order.jar").getURL());

		assertThat(pu1.excludeUnlistedClasses()).isFalse();

		assertThat(pu1.getTransactionType()).isSameAs(PersistenceUnitTransactionType.RESOURCE_LOCAL);

		Properties props = pu1.getProperties();
		assertThat(props.keySet().size()).isEqualTo(2);
		assertThat(props.getProperty("com.acme.persistence.sql-logging")).isEqualTo("on");
		assertThat(props.getProperty("foo")).isEqualTo("bar");

		assertThat(pu1.getNonJtaDataSource()).isNull();

		assertThat(pu1.getJtaDataSource()).isSameAs(ds);

		assertThat(pu1.excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();

		PersistenceUnitInfo pu2 = info[1];

		assertThat(pu2.getTransactionType()).isSameAs(PersistenceUnitTransactionType.JTA);
		assertThat(pu2.getPersistenceProviderClassName()).isEqualTo("com.acme.AcmePersistence");

		assertThat(pu2.getMappingFileNames().size()).isEqualTo(1);
		assertThat(pu2.getMappingFileNames().get(0)).isEqualTo("order2.xml");

		// the following assertions fail only during coverage runs
		// assertEquals(1, pu2.getJarFileUrls().size());
		// assertEquals(new ClassPathResource("order-supplemental.jar").getURL(), pu2.getJarFileUrls().get(0));

		assertThat(pu2.excludeUnlistedClasses()).isTrue();

		assertThat(pu2.getJtaDataSource()).isNull();
		assertThat(pu2.getNonJtaDataSource()).isEqualTo(ds);

		assertThat(pu2.excludeUnlistedClasses()).as("Exclude unlisted should be true when no value.").isTrue();
	}

	@Test
	public void testExample6() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-example6.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);
		assertThat(info.length).isEqualTo(1);
		assertThat(info[0].getPersistenceUnitName()).isEqualTo("pu");
		assertThat(info[0].getProperties().keySet().size()).isEqualTo(0);

		assertThat(info[0].excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();
	}

	@Disabled("not doing schema parsing anymore for JPA 2.0 compatibility")
	@Test
	public void testInvalidPersistence() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-invalid.xml";
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
				reader.readPersistenceUnitInfos(resource));
	}

	@Disabled("not doing schema parsing anymore for JPA 2.0 compatibility")
	@Test
	public void testNoSchemaPersistence() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-no-schema.xml";
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
				reader.readPersistenceUnitInfos(resource));
	}

	@Test
	public void testPersistenceUnitRootUrl() throws Exception {
		URL url = PersistenceUnitReader.determinePersistenceUnitRootUrl(new ClassPathResource("/org/springframework/orm/jpa/persistence-no-schema.xml"));
		assertThat(url).isNull();

		url = PersistenceUnitReader.determinePersistenceUnitRootUrl(new ClassPathResource("/org/springframework/orm/jpa/META-INF/persistence.xml"));
		assertThat(url.toString().endsWith("/org/springframework/orm/jpa")).as("the containing folder should have been returned").isTrue();
	}

	@Test
	public void testPersistenceUnitRootUrlWithJar() throws Exception {
		ClassPathResource archive = new ClassPathResource("/org/springframework/orm/jpa/jpa-archive.jar");
		String newRoot = "jar:" + archive.getURL().toExternalForm() + "!/META-INF/persist.xml";
		Resource insideArchive = new UrlResource(newRoot);
		// make sure the location actually exists
		assertThat(insideArchive.exists()).isTrue();
		URL url = PersistenceUnitReader.determinePersistenceUnitRootUrl(insideArchive);
		assertThat(archive.getURL().sameFile(url)).as("the archive location should have been returned").isTrue();
	}

	@Test
	public void testJpa1ExcludeUnlisted() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-exclude-1.0.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertThat(info).isNotNull();
		assertThat(info.length).as("The number of persistence units is incorrect.").isEqualTo(4);

		PersistenceUnitInfo noExclude = info[0];
		assertThat(noExclude).as("noExclude should not be null.").isNotNull();
		assertThat(noExclude.getPersistenceUnitName()).as("noExclude name is not correct.").isEqualTo("NoExcludeElement");
		assertThat(noExclude.excludeUnlistedClasses()).as("Exclude unlisted should default false in 1.0.").isFalse();

		PersistenceUnitInfo emptyExclude = info[1];
		assertThat(emptyExclude).as("emptyExclude should not be null.").isNotNull();
		assertThat(emptyExclude.getPersistenceUnitName()).as("emptyExclude name is not correct.").isEqualTo("EmptyExcludeElement");
		assertThat(emptyExclude.excludeUnlistedClasses()).as("emptyExclude should be true.").isTrue();

		PersistenceUnitInfo trueExclude = info[2];
		assertThat(trueExclude).as("trueExclude should not be null.").isNotNull();
		assertThat(trueExclude.getPersistenceUnitName()).as("trueExclude name is not correct.").isEqualTo("TrueExcludeElement");
		assertThat(trueExclude.excludeUnlistedClasses()).as("trueExclude should be true.").isTrue();

		PersistenceUnitInfo falseExclude = info[3];
		assertThat(falseExclude).as("falseExclude should not be null.").isNotNull();
		assertThat(falseExclude.getPersistenceUnitName()).as("falseExclude name is not correct.").isEqualTo("FalseExcludeElement");
		assertThat(falseExclude.excludeUnlistedClasses()).as("falseExclude should be false.").isFalse();
	}

	@Test
	public void testJpa2ExcludeUnlisted() throws Exception {
		PersistenceUnitReader reader = new PersistenceUnitReader(
				new PathMatchingResourcePatternResolver(), new JndiDataSourceLookup());
		String resource = "/org/springframework/orm/jpa/persistence-exclude-2.0.xml";
		PersistenceUnitInfo[] info = reader.readPersistenceUnitInfos(resource);

		assertThat(info).isNotNull();
		assertThat(info.length).as("The number of persistence units is incorrect.").isEqualTo(4);

		PersistenceUnitInfo noExclude = info[0];
		assertThat(noExclude).as("noExclude should not be null.").isNotNull();
		assertThat(noExclude.getPersistenceUnitName()).as("noExclude name is not correct.").isEqualTo("NoExcludeElement");
		assertThat(noExclude.excludeUnlistedClasses()).as("Exclude unlisted still defaults to false in 2.0.").isFalse();

		PersistenceUnitInfo emptyExclude = info[1];
		assertThat(emptyExclude).as("emptyExclude should not be null.").isNotNull();
		assertThat(emptyExclude.getPersistenceUnitName()).as("emptyExclude name is not correct.").isEqualTo("EmptyExcludeElement");
		assertThat(emptyExclude.excludeUnlistedClasses()).as("emptyExclude should be true.").isTrue();

		PersistenceUnitInfo trueExclude = info[2];
		assertThat(trueExclude).as("trueExclude should not be null.").isNotNull();
		assertThat(trueExclude.getPersistenceUnitName()).as("trueExclude name is not correct.").isEqualTo("TrueExcludeElement");
		assertThat(trueExclude.excludeUnlistedClasses()).as("trueExclude should be true.").isTrue();

		PersistenceUnitInfo falseExclude = info[3];
		assertThat(falseExclude).as("falseExclude should not be null.").isNotNull();
		assertThat(falseExclude.getPersistenceUnitName()).as("falseExclude name is not correct.").isEqualTo("FalseExcludeElement");
		assertThat(falseExclude.excludeUnlistedClasses()).as("falseExclude should be false.").isFalse();
	}

}
