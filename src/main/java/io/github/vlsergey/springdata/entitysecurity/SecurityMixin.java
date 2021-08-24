package io.github.vlsergey.springdata.entitysecurity;

public interface SecurityMixin<T> {

	Condition<T> buildCondition();

}
