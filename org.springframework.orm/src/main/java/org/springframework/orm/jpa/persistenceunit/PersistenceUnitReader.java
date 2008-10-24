/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jpa.persistenceunit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.util.xml.SimpleSaxErrorHandler;

/**
 * Internal helper class for reading <code>persistence.xml</code> files.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0
 */
class PersistenceUnitReader {

	private static final String MAPPING_FILE_NAME = "mapping-file";

	private static final String JAR_FILE_URL = "jar-file";

	private static final String MANAGED_CLASS_NAME = "class";

	private static final String PROPERTIES = "properties";

	private static final String PROVIDER = "provider";

	private static final String EXCLUDE_UNLISTED_CLASSES = "exclude-unlisted-classes";

	private static final String NON_JTA_DATA_SOURCE = "non-jta-data-source";

	private static final String JTA_DATA_SOURCE = "jta-data-source";

	private static final String TRANSACTION_TYPE = "transaction-type";

	private static final String PERSISTENCE_UNIT = "persistence-unit";

	private static final String UNIT_NAME = "name";

	private static final String META_INF = "META-INF";

	private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

	private static final String SCHEMA_NAME = "persistence_1_0.xsd";

	private static final String[] SCHEMA_RESOURCE_LOCATIONS = {
			"classpath:persistence_1_0.xsd",
			"classpath:org/hibernate/ejb/persistence_1_0.xsd",
			"classpath:org/jpox/jpa/persistence_1_0.xsd"};


	private final Log logger = LogFactory.getLog(getClass());

	private final ResourcePatternResolver resourcePatternResolver;

	private final DataSourceLookup dataSourceLookup;


	/**
	 * Create a new PersistenceUnitReader.
	 * @param resourcePatternResolver the ResourcePatternResolver to use for loading resources
	 * @param dataSourceLookup the DataSourceLookup to resolve DataSource names in
	 * <code>persistence.xml</code> files against
	 */
	public PersistenceUnitReader(ResourcePatternResolver resourcePatternResolver, DataSourceLookup dataSourceLookup) {
		Assert.notNull(resourcePatternResolver, "ResourceLoader must not be null");
		Assert.notNull(dataSourceLookup, "DataSourceLookup must not be null");
		this.resourcePatternResolver = resourcePatternResolver;
		this.dataSourceLookup = dataSourceLookup;
	}


	/**
	 * Parse and build all persistence unit infos defined in the specified XML file(s).
	 * @param persistenceXmlLocation the resource location (can be a pattern)
	 * @return the resulting PersistenceUnitInfo instances
	 */
	public SpringPersistenceUnitInfo[] readPersistenceUnitInfos(String persistenceXmlLocation) {
		return readPersistenceUnitInfos(new String[] {persistenceXmlLocation});
	}

	/**
	 * Parse and build all persistence unit infos defined in the given XML files.
	 * @param persistenceXmlLocations the resource locations (can be patterns)
	 * @return the resulting PersistenceUnitInfo instances
	 */
	public SpringPersistenceUnitInfo[] readPersistenceUnitInfos(String[] persistenceXmlLocations) {
		ErrorHandler handler = new SimpleSaxErrorHandler(logger);
		List<SpringPersistenceUnitInfo> infos = new LinkedList<SpringPersistenceUnitInfo>();
		String resourceLocation = null;
		try {
			for (int i = 0; i < persistenceXmlLocations.length; i++) {
				Resource[] resources = this.resourcePatternResolver.getResources(persistenceXmlLocations[i]);
				for (Resource resource : resources) {
					resourceLocation = resource.toString();
					InputStream stream = resource.getInputStream();
					try {
						Document document = validateResource(handler, stream);
						parseDocument(resource, document, infos);
					}
					finally {
						stream.close();
					}
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Cannot parse persistence unit from " + resourceLocation, ex);
		}
		catch (SAXException ex) {
			throw new IllegalArgumentException("Invalid XML in persistence unit from " + resourceLocation, ex);
		}
		catch (ParserConfigurationException ex) {
			throw new IllegalArgumentException("Internal error parsing persistence unit from " + resourceLocation);
		}

		return infos.toArray(new SpringPersistenceUnitInfo[infos.size()]);
	}


	/**
	 * Validate the given stream and return a valid DOM document for parsing.
	 */
	protected Document validateResource(ErrorHandler handler, InputStream stream)
			throws ParserConfigurationException, SAXException, IOException {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);

		// Set schema location only if we found one inside the classpath.
		Resource schemaLocation = findSchemaResource();
		if (schemaLocation != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found schema resource: " + schemaLocation.getURL());
			}
			dbf.setValidating(true);
			dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, XMLConstants.W3C_XML_SCHEMA_NS_URI);
			dbf.setAttribute(JAXP_SCHEMA_SOURCE, schemaLocation.getURL().toString());
		}
		else {
			logger.debug("Schema resource [" + SCHEMA_NAME +
					"] not found - falling back to XML parsing without schema validation");
		}

		DocumentBuilder parser = dbf.newDocumentBuilder();
		parser.setErrorHandler(handler);
		return parser.parse(stream);
	}

	/**
	 * Try to locate the schema first in the class path before using the URL specified inside the XML.
	 * @return an existing resource, or <code>null</code> if none found
	 */
	protected Resource findSchemaResource() {
		for (int i = 0; i < SCHEMA_RESOURCE_LOCATIONS.length; i++) {
			Resource schemaLocation = this.resourcePatternResolver.getResource(SCHEMA_RESOURCE_LOCATIONS[i]);
			if (schemaLocation.exists()) {
				return schemaLocation;
			}
		}
		return null;
	}


	/**
	 * Parse the validated document and add entries to the given unit info list.
	 */
	protected List<SpringPersistenceUnitInfo> parseDocument(
			Resource resource, Document document, List<SpringPersistenceUnitInfo> infos) throws IOException {

		Element persistence = document.getDocumentElement();
		URL unitRootURL = determinePersistenceUnitRootUrl(resource);
		List<Element> units = (List<Element>) DomUtils.getChildElementsByTagName(persistence, PERSISTENCE_UNIT);
		for (Element unit : units) {
			SpringPersistenceUnitInfo info = parsePersistenceUnitInfo(unit);
			info.setPersistenceUnitRootUrl(unitRootURL);
			infos.add(info);
		}

		return infos;
	}

	/**
	 * Determine the persistence unit root URL based on the given resource
	 * (which points to the <code>persistence.xml</code> file we're reading).
	 * @param resource the resource to check
	 * @return the corresponding persistence unit root URL
	 * @throws IOException if the checking failed
	 */
	protected URL determinePersistenceUnitRootUrl(Resource resource) throws IOException {
		URL originalURL = resource.getURL();
		String urlToString = originalURL.toExternalForm();

		// If we get an archive, simply return the jar URL (section 6.2 from the JPA spec)
		if (ResourceUtils.isJarURL(originalURL)) {
			return ResourceUtils.extractJarFileURL(originalURL);
		}

		else {
			// check META-INF folder
			if (!urlToString.contains(META_INF)) {
				if (logger.isInfoEnabled()) {
					logger.info(resource.getFilename() +
							" should be located inside META-INF directory; cannot determine persistence unit root URL for " +
							resource);
				}
				return null;
			}
			if (urlToString.lastIndexOf(META_INF) == urlToString.lastIndexOf('/') - (1 + META_INF.length())) {
				if (logger.isInfoEnabled()) {
					logger.info(resource.getFilename() +
							" is not located in the root of META-INF directory; cannot determine persistence unit root URL for " +
							resource);
				}
				return null;
			}

			String persistenceUnitRoot = urlToString.substring(0, urlToString.lastIndexOf(META_INF));
			return new URL(persistenceUnitRoot);
		}
	}

	/**
	 * Parse the unit info DOM element.
	 */
	protected SpringPersistenceUnitInfo parsePersistenceUnitInfo(Element persistenceUnit) throws IOException {
		SpringPersistenceUnitInfo unitInfo = new SpringPersistenceUnitInfo();

		// set unit name
		unitInfo.setPersistenceUnitName(persistenceUnit.getAttribute(UNIT_NAME).trim());

		// set transaction type
		String txType = persistenceUnit.getAttribute(TRANSACTION_TYPE).trim();
		if (StringUtils.hasText(txType)) {
			unitInfo.setTransactionType(PersistenceUnitTransactionType.valueOf(txType));
		}

		// data-source
		String jtaDataSource = DomUtils.getChildElementValueByTagName(persistenceUnit, JTA_DATA_SOURCE);
		if (StringUtils.hasText(jtaDataSource)) {
			unitInfo.setJtaDataSource(this.dataSourceLookup.getDataSource(jtaDataSource.trim()));
		}

		String nonJtaDataSource = DomUtils.getChildElementValueByTagName(persistenceUnit, NON_JTA_DATA_SOURCE);
		if (StringUtils.hasText(nonJtaDataSource)) {
			unitInfo.setNonJtaDataSource(this.dataSourceLookup.getDataSource(nonJtaDataSource.trim()));
		}

		// provider
		String provider = DomUtils.getChildElementValueByTagName(persistenceUnit, PROVIDER);
		if (StringUtils.hasText(provider)) {
			unitInfo.setPersistenceProviderClassName(provider.trim());
		}

		// exclude unlisted classes
		Element excludeUnlistedClasses = DomUtils.getChildElementByTagName(persistenceUnit, EXCLUDE_UNLISTED_CLASSES);
		if (excludeUnlistedClasses != null) {
			unitInfo.setExcludeUnlistedClasses(true);
		}

		parseMappingFiles(persistenceUnit, unitInfo);
		parseJarFiles(persistenceUnit, unitInfo);
		parseClass(persistenceUnit, unitInfo);
		parseProperty(persistenceUnit, unitInfo);

		return unitInfo;
	}

	/**
	 * Parse the <code>property</code> XML elements.
	 */
	@SuppressWarnings("unchecked")
	protected void parseProperty(Element persistenceUnit, SpringPersistenceUnitInfo unitInfo) {
		Element propRoot = DomUtils.getChildElementByTagName(persistenceUnit, PROPERTIES);
		if (propRoot == null) {
			return;
		}
		List<Element> properties = DomUtils.getChildElementsByTagName(propRoot, "property");
		for (Element property : properties) {
			String name = property.getAttribute("name");
			String value = property.getAttribute("value");
			unitInfo.addProperty(name, value);
		}
	}

	/**
	 * Parse the <code>class</code> XML elements.
	 */
	@SuppressWarnings("unchecked")
	protected void parseClass(Element persistenceUnit, SpringPersistenceUnitInfo unitInfo) {
		List<Element> classes = DomUtils.getChildElementsByTagName(persistenceUnit, MANAGED_CLASS_NAME);
		for (Element element : classes) {
			String value = DomUtils.getTextValue(element).trim();
			if (StringUtils.hasText(value))
				unitInfo.addManagedClassName(value);
		}
	}

	/**
	 * Parse the <code>jar-file</code> XML elements.
	 */
	@SuppressWarnings("unchecked")
	protected void parseJarFiles(Element persistenceUnit, SpringPersistenceUnitInfo unitInfo) throws IOException {
		List<Element> jars = DomUtils.getChildElementsByTagName(persistenceUnit, JAR_FILE_URL);
		for (Element element : jars) {
			String value = DomUtils.getTextValue(element).trim();
			if (StringUtils.hasText(value)) {
				Resource[] resources = this.resourcePatternResolver.getResources(value);
				for (int i = 0; i < resources.length; i++) {
					unitInfo.addJarFileUrl(resources[i].getURL());
				}
			}
		}
	}

	/**
	 * Parse the <code>mapping-file</code> XML elements.
	 */
	@SuppressWarnings("unchecked")
	protected void parseMappingFiles(Element persistenceUnit, SpringPersistenceUnitInfo unitInfo) {
		List<Element> files = DomUtils.getChildElementsByTagName(persistenceUnit, MAPPING_FILE_NAME);
		for (Element element : files) {
			String value = DomUtils.getTextValue(element).trim();
			if (StringUtils.hasText(value)) {
				unitInfo.addMappingFileName(value);
			}
		}
	}

}
