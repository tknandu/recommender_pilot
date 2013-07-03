/** DJ **/
package org.recommender101.tools;

/**
 * Must be implemented by all reocmmender and evaluator classes instantiated by the
 * framework
 * @author DJ
 *
 */
public abstract class Instantiable {
	
	String configurationFileString;

	/**
	 * Sets what was specified in the configuration file 
	 */
	public void setConfigurationFileString(String s) {
		this.configurationFileString = s;
	}
	
	/**
	 * Returns what was specified in the configuration file
	 * @return
	 */
	public String getConfigurationFileString() {
		return configurationFileString;
	}
	
	/**
	 * A default toString() method
	 */
	public String toString() {
		return configurationFileString;
	}
}
