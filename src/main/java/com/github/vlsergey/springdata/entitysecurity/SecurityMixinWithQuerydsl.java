package com.github.vlsergey.springdata.entitysecurity;

import javax.annotation.concurrent.ThreadSafe;

@FunctionalInterface
@ThreadSafe
public interface SecurityMixinWithQuerydsl<T> extends SecurityMixin<T> {

	@Override
	ConditionWithQuerydsl<T> buildCondition();

}
