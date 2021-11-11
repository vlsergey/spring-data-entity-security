package com.github.vlsergey.springdata.entitysecurity.owned;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ContextConfiguration(classes = TestConfiguration.class)
class WrongOwnerTest {

	private UUID notMyEntityId;

	@Autowired
	private OwnedTestEntityRepository testRepository;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@BeforeEach
	@Rollback(false)
	@Transactional
	public void createFooEntity() {
		SecurityContextHolder.setContext(new SecurityContextImpl());
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("foo", null, emptyList()));
		OwnedTestEntity fooItem = new OwnedTestEntity();
		fooItem.setOwner("foo");
		fooItem.setValue(42);
		fooItem = testRepository.save(fooItem);
		notMyEntityId = fooItem.getId();
		testRepository.flush();

		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("bar", null, emptyList()));
	}

	@Test
	void deleteByIdReturnsFalse() {
		assertThrows(EmptyResultDataAccessException.class, () -> testRepository.deleteById(notMyEntityId));
	}

	@Test
	void existsByIdReturnsFalse() {
		assertFalse(testRepository.existsById(notMyEntityId));
	}

	@Test
	void saveAndFlushNewItemWithWrondIdThrowsException() {
		OwnedTestEntity barItem = new OwnedTestEntity();
		barItem.setOwner("bar");
		barItem.setValue(84);

		OwnedTestEntity afterSave = testRepository.save(barItem);
		afterSave.setId(notMyEntityId);

		// throws exception that another entity with this ID exists in DB
		assertThrows(JpaSystemException.class, () -> testRepository.saveAndFlush(afterSave));
	}

	@Test
	void saveExistingItemWithChangedAndWrondIdThrowsException() {
		OwnedTestEntity barItem = new OwnedTestEntity();
		barItem.setOwner("bar");
		barItem.setValue(84);

		OwnedTestEntity afterSave = testRepository.saveAndFlush(barItem);
		afterSave.setId(notMyEntityId);

		// throws exception that another entity with this ID exists in DB
		assertThrows(JpaSystemException.class, () -> testRepository.save(afterSave));
	}

	@Test
	void saveNewItemWithCorrectIdIsOkay() {
		OwnedTestEntity barItem = new OwnedTestEntity();
		barItem.setOwner("bar");
		barItem.setValue(84);
		barItem = testRepository.save(barItem);
		assertNotNull(barItem);
	}

	@Test
	void saveNewItemWithWrondIdThrowsException() {
		final OwnedTestEntity barItem = new OwnedTestEntity();
		barItem.setId(notMyEntityId);
		barItem.setOwner("bar");
		barItem.setValue(84);

		assertThrows(AccessDeniedException.class, () -> {
			testRepository.save(barItem);
		});
	}

}
