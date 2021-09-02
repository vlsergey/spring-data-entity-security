package com.github.vlsergey.springdata.entitysecurity.simple;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = TestConfiguration.class)
class SimpleTestEntityTest {

	@Autowired
	private SimpleTestEntityRepository testRepository;

	@Test
	void newEntityIsDeletedNotOldOneOnIdChange() {
		SimpleTestEntity old = new SimpleTestEntity();
		old.setId("oldId");
		old.setValue(42);
		old = testRepository.save(old);

		SimpleTestEntity newEntity = new SimpleTestEntity();
		newEntity.setId("newId");
		newEntity.setValue(84);
		newEntity = testRepository.save(newEntity);

		newEntity.setId(old.getId());

		testRepository.delete(newEntity);
		assertTrue(testRepository.existsById(old.getId()));
	}

	@Test
	void savingTwoEntitiesWithSameIdIsJustUpdate() {
		SimpleTestEntity test1 = new SimpleTestEntity();
		test1.setId("oldId");
		test1.setValue(42);
		test1 = testRepository.save(test1);
		testRepository.flush();

		SimpleTestEntity test2 = new SimpleTestEntity();
		test2.setId("oldId");
		test2.setValue(84);
		test1 = testRepository.save(test2);
		testRepository.flush();
	}

}
