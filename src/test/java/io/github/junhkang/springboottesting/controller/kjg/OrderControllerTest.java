package io.github.junhkang.springboottesting.controller.kjg;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.github.junhkang.springboottesting.controller.OrderController;
import io.github.junhkang.springboottesting.domain.Order;
import io.github.junhkang.springboottesting.domain.OrderStatus;
import io.github.junhkang.springboottesting.service.OrderService;

@WebMvcTest(OrderController.class) // 컨트롤러 계층만 로딩하여 테스트. 이외 서비스는 MockBean 처리
@DisplayName("OrderController 테스트")
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc; // 수동 주입 필요

	@MockBean
	private OrderService orderService; // MockBean 처리

	@DisplayName("모든 주문 조회 테스트")
	@Test
	void testGetAllOrders() throws Exception {
		Order order = new Order();
		order.setId(1L);
		order.setStatus(OrderStatus.PENDING);
		Mockito.when(orderService.getAllOrders()).thenReturn(Collections.singletonList(order));

		// When & Then: GET 요청을 수행하고 응답을 검증
		mockMvc.perform(get("/orders"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id", is(1)))
			.andExpect(jsonPath("$[0].status", is("PENDING")));
	}

	@DisplayName("주문 ID로 주문 조회 테스트")
	@Test
	void testGetOrderById() throws Exception {
		// given : 서비스 계층 모킹
		Order order = new Order();
		order.setId(1L);
		order.setStatus(OrderStatus.PENDING);

		// 메서드 응답 모킹
		Mockito.when(orderService.getOrderById(1L)).thenReturn(order);

		// when & then : GET 요청 수행에 대한 응답 검증
		mockMvc.perform(get("/orders/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id", is(1)))
			.andExpect(jsonPath("$.status", is("PENDING")));
	}

	@DisplayName("주문 생성 테스트")
	@Test
	void testCreateOrder() throws Exception {
		// given : 서비스 계층 모킹
		Order order = new Order();
		order.setId(1L);
		order.setStatus(OrderStatus.PENDING);

		// 파라미터도 모킹 처리 - ArgumentMatchers
		Mockito.when(orderService.createOrder(anyLong(), anyLong(), any())).thenReturn(order);

		// when & then
		mockMvc.perform(post("/orders")
				.param("userId", "1")
				.param("productId", "1")
				.param("quantity", "2")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id", is(1)))
			.andExpect(jsonPath("$.status", is("PENDING")));
	}

	@DisplayName("주문 취소 테스트")
	@Test
	void testCancelOrder() throws Exception {
		// given
		Order order = new Order();
		order.setId(1L);
		order.setStatus(OrderStatus.CANCELED);
		Mockito.when(orderService.cancelOrder(1L)).thenReturn(order);

		// when & then
		mockMvc.perform(delete("/orders/1/cancel"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id", is(1)))
			.andExpect(jsonPath("$.status", is("CANCELED")));

	}

	@DisplayName("주문 수량 업데이트 테스트")
	@Test
	void testUpdateOrderQuantity() throws Exception {
		// given :
		Order order = new Order();
		order.setId(1L);
		order.setQuantity(5);
		Mockito.when(orderService.updateOrderQuantity(anyLong(), any())).thenReturn(order);

		// when & then : PUT 요청 수행 후 응답 검증
		mockMvc.perform(put("/orders/1/quantity")
				.param("newQuantity", "5")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id", is(1)))
			.andExpect(jsonPath("$.quantity", is(5)));
	}

	@DisplayName("사용자 ID로 주문 조회 테스트")
	@Test
	void testGetOrdersByUserId() throws Exception {
		// given : 서비스 계층 모킹
		Order order = new Order();
		order.setId(1L);
		order.setStatus(OrderStatus.PENDING);
		Mockito.when(orderService.getOrdersByUserId(1L)).thenReturn(Collections.singletonList(order));

		// when & then : GET 요청을 수행하고 응답을 검증
		mockMvc.perform(get("/orders/user/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id", is(1)))
			.andExpect(jsonPath("$[0].status", is("PENDING")));
	}

	@DisplayName("주문 날짜 범위로 주문 조회 테스트")
	@Test
	void testGetOrdersByDateRange() throws Exception {
		// given
		Order order1 = new Order();
		order1.setId(1L);
		Order order2 = new Order();
		order2.setId(2L);

		LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
		LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);

		// 메서드 호출 결과를 검증하는게 아님. 컨트롤러가 해당 메서드 호출을 잘 했는지 검증..
		Mockito.when(orderService.getOrdersByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(Arrays.asList(order1, order2));

		// when & then
		// 벙위 내에 속한 주문인지 아닌지 여부는 서비스에서 검증.
		mockMvc.perform(get("/orders/date")
				.param("startDate", "2023-01-01T00:00")
				.param("endDate", "2023-12-31T23:59"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id", is(1)))
			.andExpect(jsonPath("$[1].id", is(2)));
	}

	@DisplayName("주문 금액 계산 테스트")
	@Test
	void testCalculateTotalAmount() throws Exception {
		// given
		Mockito.when(orderService.calculateTotalAmount(1L)).thenReturn(500.0);

		// when & then
		mockMvc.perform(get("/orders/1/totalAmount"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", is(500.0)));
	}

}
