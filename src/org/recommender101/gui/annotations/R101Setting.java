package org.recommender101.gui.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface R101Setting {
	
	/**
	 * The internal name of the setting, written in the exact same way as in the properties file.
	 * The GUI will use this name to build the properties file
	 */
	public String name();
	
	public String displayName() default "unknown";

	public String description() default "";

	public enum SettingsType { BOOLEAN, INTEGER, TEXT, ARRAY, DOUBLE, FILE }
	SettingsType type() default SettingsType.TEXT;
	
	// If the type is integer or double, this allows setting min and max values
	public double minValue() default 0;
	public double maxValue() default Double.MAX_VALUE;
	
	// if the type is array, the following String array should be used to set the possible values
	public String[] values() default {};
	
	public String defaultValue() default "";
}
