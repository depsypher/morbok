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
 * The default log name is "log" which can be overridden by providing a name.
 *
 * @author rayvanderborght
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Logger
{
    String name() default "log";
}
