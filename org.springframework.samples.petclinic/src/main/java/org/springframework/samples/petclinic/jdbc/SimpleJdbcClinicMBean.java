package org.springframework.samples.petclinic.jdbc;

/**
 * Interface that defines a cache refresh operation.
 * To be exposed for management via JMX.
 * 
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see SimpleJdbcClinic
 */
public interface SimpleJdbcClinicMBean {

	/**
	 * Refresh the cache of Vets that the Clinic is holding.
	 * @see org.springframework.samples.petclinic.Clinic#getVets()
	 * @see SimpleJdbcClinic#refreshVetsCache()
	 */
	void refreshVetsCache();

}
