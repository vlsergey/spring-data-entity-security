package com.github.vlsergey.springdata.entitysecurity;

import javax.annotation.concurrent.ThreadSafe;

@FunctionalInterface
@ThreadSafe
public interface SecurityMixin<T> {

	Condition<T> buildCondition();

}
