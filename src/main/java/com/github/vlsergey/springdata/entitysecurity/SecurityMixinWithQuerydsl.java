package com.github.vlsergey.springdata.entitysecurity;

public interface SecurityMixinWithQuerydsl<T> extends SecurityMixin<T> {

	@Override
	ConditionWithQuerydsl<T> buildCondition();

}
