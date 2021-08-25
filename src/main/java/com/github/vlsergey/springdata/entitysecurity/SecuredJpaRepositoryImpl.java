package com.github.vlsergey.springdata.entitysecurity;

public interface SecuredJpaRepositoryImpl<T> {

	void setSecurityMixin(@lombok.NonNull @org.springframework.lang.NonNull SecurityMixin<T> securityMixin);

}
