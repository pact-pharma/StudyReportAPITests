package com.testrail.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This class creates UseAsTestRailEnabled annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UseAsTestRailEnabled
{
    boolean isTestRailEnabled() default false;
    String[] tags() default "";
}
