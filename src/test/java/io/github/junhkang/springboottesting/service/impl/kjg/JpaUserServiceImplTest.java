package io.github.junhkang.springboottesting.service.impl.kjg;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import io.github.junhkang.springboottesting.domain.User;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.jpa.UserRepository;
import io.github.junhkang.springboottesting.service.impl.JpaUserServiceImpl;
import jakarta.transaction.Transactional;

@DataJpaTest // 성공
//@SpringBootTest // 실패
@Import(JpaUserServiceImpl.class)
@ActiveProfiles("jpa")
class JpaUserServiceImplTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JpaUserServiceImpl userService;

	private User testUser;

	/**
	 * 테스트 전 데이터 초기화
	 * @BeforeEach 어노테이션을 사용하여 각 테스트 메서드 실행 전에 실행
	 */

	@BeforeEach
	void setUp() {
		// given : 테스트 시 사용할 사용자 생성 및 저장
		testUser = new User();
		testUser.setUsername("test_user");
		testUser.setEmail("test.user@example.com");

		userRepository.save(testUser);
	}

	/**
	 * 조회 관련 테스트 그룹
	 */

	@Nested
	@DisplayName("조회 관련 테스트")
	class RetrievalTests {

		@DisplayName("모든 사용자 조회 테스트")
		@Test
		void testGetAllUsers() {
			// given : data.sql에서 미리 생성된 사용자들이 존재한다고 가정

			// when
			List<User> users = userService.getAllUsers();

			// then   3 + 1 = 4
			assertThat(users).hasSize(4);
		}

		@DisplayName("사용자 ID로 사용자 조회 테스트 - 존재하는 ID")
		@Test
		void testGetUserByIdExists() {
			// given : 테스트에서 생성한 사용자의 ID

			// when : 존재하는 사용자 ID로 사용자를 조회
			User foundUser = userService.getUserById(testUser.getId());

			// then : 존재 여부, 상세 정보 확인
			assertThat(foundUser).isNotNull();
			assertThat(foundUser.getId()).isEqualTo(testUser.getId());
			assertThat(foundUser.getUsername()).isEqualTo("test_user");
			assertThat(foundUser.getEmail()).isEqualTo("test.user@example.com");
		}

		@DisplayName("사용자 ID로 사용자 조회 테스트 - 존재하지 않는 ID")
		@Test
		void testGetUserByIdNotExists() {
			// given : 존재하지 않는 사용자 ID
			long nonExistentId = 999L;

			// when & then : 사용자 조회 시 ResourceNotFoundException 이 발생하는지
			ResourceNotFoundException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResourceNotFoundException.class, () -> userService.getUserById(nonExistentId));

			assertThat(exception.getMessage()).isEqualTo("User not found with id " + nonExistentId);
		}
	}

	/**
	 * 생성 관련 테스트 그룹
	 */
	@Nested
	@DisplayName("생성 관련 테스트")
	class CreationTests {

		@DisplayName("사용자 생성 테스트 - 성공 케이스")
		@Test
		@Transactional
			// 서비스에 안붙이고 개별 메서드에 붙이네
		void testCreateUserSuccess() {
			// given
			User newUser = new User();
			newUser.setUsername("new_user");
			newUser.setEmail("new.user@example.com");

			// when
			User createdUser = userService.createUser(newUser);

			// then : 사용자가 정상적으로 저장되었는지 검증
			assertThat(createdUser).isNotNull();
			assertThat(createdUser.getId()).isNotNull();
			assertThat(createdUser.getUsername()).isEqualTo("new_user");
			assertThat(createdUser.getEmail()).isEqualTo("new.user@example.com");

			// then : 데이터베이스에 저장된 사용자 수가 증가했는지 검증
			List<User> users = userService.getAllUsers();
			assertThat(users).hasSize(5);
		}

		@Nested
		@DisplayName("사용자 생성 테스트 - 실패 케이스")
		class CreateUserFailureTests {

			@DisplayName("사용자 생성 테스트 - 필수 필드 누락(사용자 이름)")
			@Test
			void testCreateUserWithMissingUsername() {
				// given
				User incompleteUser = new User();
				incompleteUser.setEmail("incomplete.user@example.com");

				// when & then : IllegalArgumentExcpetion 발생 확인
				IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
					() -> userService.createUser(incompleteUser));

				assertThat(exception.getMessage()).isEqualTo("Username is required.");
			}

			@DisplayName("사용자 생성 테스트 - 필수 필드 누락 (이메일)")
			@Test
			void testCreateUserWithMissingEmail() {
				// given : 잘못된 이메일 형식의 사용자 정보
				User invalidEmailUser = new User();
				invalidEmailUser.setUsername("incomplete_user");

				// when & then : 사용자 생성 시 IllegalArgumentException 이 발생
				IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
					() -> userService.createUser(invalidEmailUser));

				assertThat(exception.getMessage()).isEqualTo("Email is required.");
			}

			@DisplayName("사용자 생성 테스트 - 잘못된 이메일 형식")
			@Test
			void testCreateUserWithInvalidEmail() {
				// given
				User invalidEmailUser = new User();
				invalidEmailUser.setUsername("invalid_email_user");
				invalidEmailUser.setEmail("invali-email");

				// when & then : 사용자 생성 시 IllegalArugmentExcpetion 발생
				IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
					() -> userService.createUser(invalidEmailUser));

				assertThat(exception.getMessage()).isEqualTo("Invalid email format.");
			}
		}
	}
}


