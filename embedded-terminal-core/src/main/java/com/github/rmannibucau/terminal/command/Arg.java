package com.github.rmannibucau.terminal.command;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Arg {
    String value() default "";
    boolean required() default false;
    String description() default "";
}
