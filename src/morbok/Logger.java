/*
 * $Id$
 * $URL$
 */
package morbok;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a logger named after the fully qualified class name.
 * <p>
 * The log variable is declared private static final.
 * The default log variable name is "log" which can be overridden by setting var.
 * The default log value is the fully qualified class name of the logged class, which can be overridden by providing a value.
 * The default log implementation is apache commons logging which can be overrided by providing a log type.
 *
 * @author rayvanderborght
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Logger
{
    String var() default "log";
    String value() default "";
    morbok.Logger.Type type() default morbok.Logger.Type.COMMONS;

    /** */
    public static enum Type
    {
        COMMONS,
        JAVA,
        SLF4J
    }
}
