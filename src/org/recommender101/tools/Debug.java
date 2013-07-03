/** DJ **/
package org.recommender101.tools;

import org.recommender101.eval.impl.Recommender101Impl;

/**
 * A debug helper
 * @author DJ
 *
 */
public class Debug {
	
  // =====================================================================================

	static public boolean DEBUGGING_ON = false;
	
	static {
		if (Recommender101Impl.properties != null) {
			String debugOnStr = Recommender101Impl.properties.getProperty("Debug.Messages");
			if ("on".equalsIgnoreCase(debugOnStr)) {
				DEBUGGING_ON = true;
			}
			else if  ("off".equalsIgnoreCase(debugOnStr)) {
				DEBUGGING_ON = false;
			}
		}
	}
	
	
	/**
	 * Simply prints stuff
	 */
	public static void log(String msg) {
		if (DEBUGGING_ON) {
			System.out.println("[DEBUG] "  + msg);
		}
	}
	
	/**
	 * prints error stuff
	 */
	public static void error(String msg) {
		if (DEBUGGING_ON) {
			System.err.println("[ERROR] "  + msg);
		}
	}
}
