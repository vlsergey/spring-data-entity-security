package com.github.vlsergey.springdata.entitysecurity;

@FunctionalInterface
public interface SecurityMixinWithQuerydsl<T> extends SecurityMixin<T> {

	@Override
	ConditionWithQuerydsl<T> buildCondition();

}
