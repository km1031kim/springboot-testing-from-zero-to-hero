package io.github.junhkang.springboottesting.controller.kjg;

import java.util.Collections;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.github.junhkang.springboottesting.controller.UserController;
import io.github.junhkang.springboottesting.domain.User;
import io.github.junhkang.springboottesting.service.UserService;

@WebMvcTest(UserController.class)
@DisplayName("UserController 테스트")
public class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserService userService;


	@DisplayName("모든 사용자 조회 테스트")
	@Test
	void testGetAllUsers() throws Exception {
	    // given
		User user = new User();
		user.setId(1L);
		user.setUsername("test_user");
		Mockito.when(userService.getAllUsers()).thenReturn(Collections.singletonList(user));

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.get("/users"))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$[0].id", Matchers.is(1)))
			.andExpect(MockMvcResultMatchers.jsonPath("$[0].username", Matchers.is("test_user")));


	}

	@DisplayName("사용자 ID로 조회 테스트")
	@Test
	void testGetUserById() throws Exception {
	    // given
		User user = new User();
		user.setId(1L);
		user.setUsername("test_user");
		Mockito.when(userService.getUserById(ArgumentMatchers.anyLong())).thenReturn(user);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.get("/users/1"))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(1)))
			.andExpect(MockMvcResultMatchers.jsonPath("$.username", Matchers.is("test_user")));
	}

	@DisplayName("사용자 생성 테스트")
	@Test
	void testCreateUser() throws Exception {
	    // given
		User user = new User();
		user.setId(1L);
		user.setUsername("new_user");
		Mockito.when(userService.createUser(ArgumentMatchers.any(User.class))).thenReturn(user);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.post("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\" :  \"new_user\", \"email\" :  \"new_user@example.com\"}"))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(1)))
			.andExpect(MockMvcResultMatchers.jsonPath("$.username", Matchers.is("new_user")));
	}



}
