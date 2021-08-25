package com.github.vlsergey.springdata.entitysecurity;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface SecuredWith {

	Class<? extends SecurityMixin<?>> value();

}
