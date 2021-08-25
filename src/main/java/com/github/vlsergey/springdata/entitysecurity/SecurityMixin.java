package com.github.vlsergey.springdata.entitysecurity;

@FunctionalInterface
public interface SecurityMixin<T> {

	Condition<T> buildCondition();

}
