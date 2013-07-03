/** DJ **/
package org.recommender101.tools;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * A helper class that accepts a class name + parameters and instantiates the class
 * @author DJ
 *
 */
public class ClassInstantiator {
	
	public static String SEPARATOR = ":";
	public static String PARAM_SEPARATOR = "\\|";
	
  // =====================================================================================

	/**
	 * The method accepts a string containing a class name and a named parameter list of the
	 * form classname:param1=a:param2=b etc
	 * All parameters are passed as strings
	 * @param classNameAndParameters
	 * @return an instance of the class
	 */
	@SuppressWarnings("unchecked")
	public static Object instantiateClass(String classNameAndParameters)  {
		try{ 
			List<String> parameterList= new ArrayList<String>();
			int idx = -1;
			String classname = classNameAndParameters;
			idx = classNameAndParameters.indexOf(SEPARATOR);
			if (idx != -1) {
				classname = classNameAndParameters.substring(0,idx);
				String parameters = classNameAndParameters.substring(idx+1,classNameAndParameters.length());
				String[] tokens = parameters.split(PARAM_SEPARATOR);
				for (int i=0;i<tokens.length;i++) {
					parameterList.add(tokens[i]);
				}
			}
			Debug.log("ClassInstantiator: Instantiating: " + classname + " " + parameterList);
			
			@SuppressWarnings("rawtypes")
			Class clazz = Class.forName(classname.trim());
			Object instance = clazz.newInstance();

			// Remember what we did here
			if (instance instanceof Instantiable) {
				Instantiable inst = (Instantiable) instance;
				inst.setConfigurationFileString(classNameAndParameters);
			}
			
			// set the parameters
			Method m = null;
			for (String param : parameterList) {
				String[] theparam = param.split("=");
				String fieldname = theparam[0];
				fieldname = fieldname.substring(0,1).toUpperCase() + fieldname.substring(1);
//				Debug.log("ClassInstantiator: Setting field: " + fieldname + "=" + theparam[1]);
				m = clazz.getMethod("set" + fieldname, String.class);
				
				if (m != null) {
					// Calling the setter
					m.invoke(instance, theparam[1]);
				}
			}
			return instance;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("FATAL ERROR WHEN LOADING CLASSES:  " + classNameAndParameters);
			System.out.println("---------- PROGRAM TERMINATED --------------- ");
			System.exit(1);
			return null;
		}
	};

  // =====================================================================================

	/**
	 * Creates an instance given a property string
	 * @param propertyString
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Object instantiateClassByProperty(Properties properties, String propertyString, Class defaultClass) throws Exception {
		// Get the data loader class
		String thePropertyString = properties.getProperty(propertyString);
		Object instance = null;
		if (thePropertyString != null) {
			instance = ClassInstantiator.instantiateClass(thePropertyString);
			if (instance == null) {
				throw new Exception("Custom object could not be instantiated: " + thePropertyString);
			}
			else {
				return instance;
			}
		}
		if (defaultClass != null) {
			// try the default class
			try {
				instance = defaultClass.newInstance();
				return instance;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	
  // =====================================================================================

	/**
	 * A helper to load multiple properties
	 * @param property
	 * @param clazz
	 * @param names an optional parameter where the original strings can be stored (pass through)
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List instantiateClassesByProperties(String propertyString) throws Exception {
		
		
		List result = new ArrayList();
		String[] objStrings = propertyString.split(",");
		
		// DJ: Check for duplicates and fail in case of a duplicate
		Set<String> strings = new HashSet<String>();
		for (String str : objStrings) {
			if (strings.contains(str.trim())) {
				throw new Exception ("FATAL CONFIGURATIN ERROR - Duplicate entry: " + str);
			}
			strings.add(str.trim());
		}

		Object object;
		for (String str : objStrings) {
//			Debug.log("Recommender101:: instantiateClassesByProperties |" + str + "|");
			object = ClassInstantiator.instantiateClass(str.trim());
			result.add(object);
		}
		return result;
		
	}
}
