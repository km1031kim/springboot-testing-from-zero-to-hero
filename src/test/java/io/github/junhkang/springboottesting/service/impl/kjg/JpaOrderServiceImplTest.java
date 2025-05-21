package io.github.junhkang.springboottesting.service.impl.kjg;


import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import io.github.junhkang.springboottesting.domain.Order;
import io.github.junhkang.springboottesting.domain.OrderStatus;
import io.github.junhkang.springboottesting.domain.Product;
import io.github.junhkang.springboottesting.domain.User;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.jpa.OrderRepository;
import io.github.junhkang.springboottesting.repository.jpa.ProductRepository;
import io.github.junhkang.springboottesting.repository.jpa.UserRepository;
import io.github.junhkang.springboottesting.service.impl.JpaOrderServiceImpl;
import jakarta.persistence.Id;

/**
 * JpaOrderServiceImpl 서비스 구현체의 비즈니스 로직 검증을 위한 단위 테스트
 * @DataJpaTest 어노테이션으로 JPA 관련 컴포넌트만 로드, @ActiveProfile("jpa") 를 통해 jpa 프로파일 활성화
 */

@DataJpaTest
@Import(JpaOrderServiceImpl.class) // 컴포넌트 스캔 대상이 아닌 경우 직접 Import.. 대상 클래스 명시 용도로 쓴듯.
@ActiveProfiles("jpa")
class JpaOrderServiceImplTest {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private JpaOrderServiceImpl orderService;

	private User testUser;
	private Product testProduct;

	/**
	 * 테스트 전 데이터 초기화
	 * @BeforeEach 어노테이션을 통해 각 테스트 메서드 실행 전 테스트에 필요한 사용자, 상품 생성 및 저장
	 */

	@BeforeEach
	void setUp() {

		// given : 테스트에 필요한 사용자 생성 및 저장
		testUser = new User();
		testUser.setUsername("test_user");
		testUser.setEmail("test.user@example.com");
		userRepository.save(testUser);

		// given : 테스트에 사용할 상품 생성 및 저장
		testProduct = new Product();
		testProduct.setName("Test Product");
		testProduct.setDescription("Test Description");
		testProduct.setPrice(100.0);
		testProduct.setStock(50);
		productRepository.save(testProduct);
	}

	@Nested
	@DisplayName("조회 관련 테스트")
	class RetrievalTests {

		@DisplayName("모든 주문 조회 테스트")
		@Test
		void testGetAllOrders() {
			// given : 두 개의 주문을 생성 및 저장
			Order order1 = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);
			Order order2 = orderService.createOrder(testUser.getId(), testProduct.getId(), 1);

			// when : 모든 주문 조회
			List<Order> orders = orderService.getAllOrders();

			// then : 데이터베이스에 저장된 주문 수 검증
			// data.sql 에서 5개의 주문이 미리 생성됨.
			assertThat(orders).hasSize(7);
		}

		@ParameterizedTest
		@ValueSource(ints = {1, 2, 3, 5, 10})
		@DisplayName("다양한 수량으로 주문 생성 테스트")
		void testCreatedOrderWithDifferentQuantities(int quantity) {

			Long userId = testUser.getId();
			Long productId = testProduct.getId();

			Order order = orderService.createOrder(userId, productId, quantity);
			assertThat(order.getQuantity()).isEqualTo(quantity);
		}

		@DisplayName("주문 ID로 주문 조회 테스트 - 존재하는 ID")
		@Test
		void testGetOrderByIdExists() {
			// given : 주문 생성 및 저장 ( 서비스 메서드 사용 )
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 3);

			// when : 존재하는 주문 ID로 주문 조회
			Order foundOrder = orderService.getOrderById(order.getId());

			// then : 주문 정상 조회 및 세부 사항 검증
			assertThat(foundOrder).isNotNull();
			assertThat(foundOrder.getId()).isEqualTo(order.getId());
			assertThat(foundOrder.getUser().getUsername()).isEqualTo("test_user");
			assertThat(foundOrder.getProduct().getName()).isEqualTo("Test Product");
			assertThat(foundOrder.getQuantity()).isEqualTo(3);
			assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
			assertThat(foundOrder.getTotalAmount()).isEqualTo(300.0);

		}

		@DisplayName("사용자 ID로 주문 조회 테스트")
		@Test
		void testGetOrdersByUserId() {
			// given : 다른 사용자 생성 및 저장
			User anotherUser = new User();
			anotherUser.setUsername("another_user");
			anotherUser.setEmail("another.user@example.com");
			userRepository.save(anotherUser);

			// given: testUser의 주문 생성 및 저장 (서비스 메서드 사용)
			Order order1 = orderService.createOrder(testUser.getId(), testProduct.getId(), 1);

			// given: anotherUser의 주문 생성 및 저장 (서비스 메서드 사용)
			Order order2 = orderService.createOrder(anotherUser.getId(), testProduct.getId(), 3);

			// when : testUser의 모든 주문 조회
			List<Order> userOrders = orderService.getOrdersByUserId(testUser.getId());

			// then : testUser의 주문만 조회되었는지 검증
			assertThat(userOrders).hasSize(1);
			assertThat(userOrders.get(0).getUser().getUsername()).isEqualTo("test_user");

		}

		@DisplayName("주문 날짜 범위로 주문 조회 테스트")
		@Test
		void testGetOrdersByDateRange() {
			// given : 주문 생성 및 저장
			Order order1 = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);
			Order order2 = orderService.createOrder(testUser.getId(), testProduct.getId(), 1);

			// 주문의 orderDate를 특정 날짜로 설정 ( 테스트 목적 )
			LocalDateTime date1 = LocalDateTime.of(2023, 1, 1, 10, 0);
			LocalDateTime date2 = LocalDateTime.of(2023, 6, 15, 15, 30);

			Order savedOrder1 = orderRepository.findById(order1.getId()).orElseThrow();
			savedOrder1.setOrderDate(date1);
			orderRepository.save(savedOrder1);

			Order savedOrder2 = orderRepository.findById(order2.getId()).orElseThrow();
			savedOrder2.setOrderDate(date2);
			orderRepository.save(savedOrder2);

			// given : data.sql에 해당 범위에 속한 주문이 이미 존재합니다.

			// when : 특정 날짜 범위를 설정하여 주문 조회
			LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
			LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);
			List<Order> ordersInRange = orderService.getOrdersByDateRange(startDate, endDate);

			// then : 설정한 날짜 범위 내에 있는 주문들이 정확히 조회되는지 검증
			assertThat(ordersInRange).hasSize(2);

			// then : 조회된 주문들의 OrderDate 검증
			assertThat(ordersInRange).allMatch(
				order -> !order.getOrderDate().isBefore(startDate) && !order.getOrderDate().isAfter(endDate));

			// 추가 검증 : 특정 주문이 포함되어있는지 확인
			assertThat(ordersInRange).extracting(Order::getId).contains(savedOrder1.getId(), savedOrder2.getId());

		}

	}

	/**
	 * 생성 및 수정 관련 테스트 그룹
	 */
	@Nested
	@DisplayName("생성 및 수정 관련 테스트")
	class CreationAndUpdateTests {
		/**
		 * 주문 생성 테스트 - 성공 케이스
		 */

		@DisplayName("주문 생성 테스트 - 성공 케이스")
		@Test
		@Transactional
		@Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
		void testCreateOrderSuccess() {
			// given : 유효한 사용자 ID, 상품 Id, 및 수량
			Long userId = testUser.getId();
			Long productId = testProduct.getId();
			Integer quantity = 5;

			// when : 주문 생성
			Order createdOrder = orderService.createOrder(userId, productId, quantity);

			// then : 주문이 정상적으로 생성되고, 관련 데이터가 올바르게 업데이트되었느지 검증
			assertThat(createdOrder).isNotNull();
			assertThat(createdOrder.getId()).isNotNull();
			assertThat(createdOrder.getUser().getId()).isEqualTo(userId);
			assertThat(createdOrder.getProduct().getId()).isEqualTo(productId);
			assertThat(createdOrder.getQuantity()).isEqualTo(quantity);
			assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
			assertThat(createdOrder.getTotalAmount()).isEqualTo(testProduct.getPrice() * quantity);

			// then : 상품의 재고가 감소했는지 검증
			Product updatedProduct = productRepository.findById(productId).orElse(null);
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(45); // 50 - 45

		}

		@DisplayName("다양한 사용자 이름 및 이메일로 주문 생성 테스트")
		@ParameterizedTest
		@CsvSource({"test_user, test.user@example.com", "john_doe, john_doe@example.com"})
		void testCreateOrderWithDifferentUsers(String username, String email) {
			// given
			User user = new User();
			user.setUsername(username);
			user.setEmail(email);
			userRepository.save(user);

			Long productId = testProduct.getId();
			Order order = orderService.createOrder(user.getId(), productId, 1);

			assertThat(order.getUser().getUsername()).isEqualTo(username);
			assertThat(order.getUser().getEmail()).isEqualTo(email);

		}

		@DisplayName("주문 생성 테스트 - 실패 케이스 ( 재고 부족 )")
		@Test
		void testCreateOrderInsufficientStock() {
			// given : 유효한 사용자 ID, 상품 Id 및 재고보다 많은 수량
			Long userId = testUser.getId();
			Long productId = testProduct.getId();
			Integer quantity = 100; // 재고 50보다 큼

			// when & then : 주문 생성 시 IllegalArgumentException 이 발생하는지 검증
			IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
				IllegalArgumentException.class, () -> {
					orderService.createOrder(userId, productId, quantity);
				});

			assertThat(exception.getMessage()).isEqualTo("Insufficient stock for product id " + productId);
		}

		@DisplayName("주문 수량 업데이트 테스트 - 성공 케이스 (증가)")
		@Test
		void testUpdateOrderQuantityIncrease() {
			// given : 주문 생성 및 저장
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

			// when : 주문 수량 업데이트
			Integer newQuantity = 4;
			Order updatedOrder = orderService.updateOrderQuantity(order.getId(), newQuantity);

			// then : 주문 수량과 총 금액이 올바르게 업데이트되었는지 검증
			assertThat(updatedOrder.getQuantity()).isEqualTo(newQuantity);
			assertThat(updatedOrder.getTotalAmount()).isEqualTo(testProduct.getPrice() * newQuantity);

			// then : 상품의 재고가 올바르게 감소했는지 검증
			Product updatedProduct = productRepository.findById(testProduct.getId()).orElse(null);
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(46);

		}

		@DisplayName("주문 수량 업데이트 테스트 - 실패 케이스 ( 재고 부족 )")
		@Test
		void testUpdateOrderQuantityInsufficientStock() {
			// given : 주문을 생성 및 저장
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

			// when & then : 주문 수량을 재고를 초과하는 값으로 업데이트 시도 시 IllegalArgumentException
			Integer newQuantity = 100;
			IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
				IllegalArgumentException.class, () -> {
					orderService.updateOrderQuantity(order.getId(), newQuantity);
				});

			assertThat(exception.getMessage()).isEqualTo("Insufficient stock to increase quantity.");

			// 주문 수량이 변경되지 않았는지 검증
			Order updatedOrder = orderRepository.findById(order.getId()).orElse(null);
			assertThat(updatedOrder).isNotNull();
			assertThat(updatedOrder.getQuantity()).isEqualTo(2);
			assertThat(updatedOrder.getTotalAmount()).isEqualTo(200.0);
		}

		@DisplayName("반복된 주문 생성 테스트 - 여러 번 주문 생성하여 성능 확인")
		@RepeatedTest(value = 5, name = "주문 생성 반복 테스트 {currentRepetition}/{totalRepetitions}")
		void testCreateOrderRepeated() {
			// given
			Long userId = testUser.getId();
			Long productId = testProduct.getId();
			Integer quantity = 3;

			// when
			Order createdOrder = orderService.createOrder(userId, productId, quantity);

			assertThat(createdOrder).isNotNull();
			assertThat(createdOrder.getQuantity()).isEqualTo(quantity);
			assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
		}
	}

	@Nested
	@DisplayName("취소 관련 테스트")
	class CancellationTests {

		@DisplayName("주문 취소 테스트 - 성공 테스트")
		@Test
		@Transactional
		void testCancelOrderSuccess() {
			// given : 주문을 생성 및 저장 (서비스 메서드 사용)
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

			// when : 주문을 취소
			Order canceledOrder = orderService.cancelOrder(order.getId());

			// then : 주문 상태가 CANCELED로 변경되었는지 검증
			assertThat(canceledOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);

			// then : 상품의 재고가 복구되었는지 검증
			Product updatedProduct = productRepository.findById(testProduct.getId()).orElse(null);
			AssertionsForClassTypes.assertThat(updatedProduct).isNotNull();
			AssertionsForClassTypes.assertThat(updatedProduct.getStock()).isEqualTo(50); // 50 - 2 + 2 = 50
		}

		@DisplayName("주문 취소 테스트 - 실패 케이스 ( 주문 상태가 PENDING이 아님")
		@Test
		void testCancelOrderNotPending() {
			// given : 주문을 생성 및 저장 ( 서비스 메서드 사용 )
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

			// given : 주문 상태를 COMPLETED 로 변경
			order.setStatus(OrderStatus.COMPLETED);
			orderRepository.save(order);

			// when & then : 주문 취소 시도 시 IllegalArgumentException 발생
			IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
				IllegalArgumentException.class, () -> {
					orderService.cancelOrder(order.getId());
				});

			assertThat(exception.getMessage()).isEqualTo("Only pending orders can be canceled.");

			// then : 주문 상태가 변경되지 않았는지 검증
			Order updatedOrder = orderRepository.findById(order.getId()).orElse(null);

			assertThat(updatedOrder).isNotNull();
			assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

			// then : 상품의 재고가 변경되지 않았는지 검증
			Product updatedProduct = productRepository.findById(testProduct.getId()).orElse(null);
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(48);
		}

	}

	@Nested
	@DisplayName("주문 금액 관련 테스트 그룹")
	class CalculateTotalAmountTests {
		/**
		 * 주문 금액 계산 테스트
		 */
		@DisplayName("주문 금액 계산 테스트")
		@Test
		void testCalculateTotalAmount() {
			// given : 주문 생성 및 저장
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 5);

			// when : 주문 총 금액 계산
			Double totalAmount = orderService.calculateTotalAmount(order.getId());

			// then : 계산된 총 금액 검증
			assertThat(totalAmount).isEqualTo(500.0);
		}

	}

	@Nested
	@DisplayName("예외 상황 관련 테스트")
	class ExceptionTests {

		@DisplayName("주문 생성 테스트 - 존재하지 않는 사용자 ID")
		@Test
		void testCreateOrderWithNonExistentUser() {
		    // given
			Long nonExistentUserId = 999L;
			Long productId = testProduct.getId();
			Integer quantity = 1;


		    // when & then : 주문 생성 시 ResourceNotFoundException 검증
			ResourceNotFoundException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResourceNotFoundException.class, () -> {
					orderService.createOrder(nonExistentUserId, productId, quantity);
				});

			assertThat(exception.getMessage()).isEqualTo("User not found with id " + nonExistentUserId);
		}

		@DisplayName("주문 생성 테스트 - 존재하지 않는 상품 ID")
		@Test
		void testCreateOrderWithNonExistentProduct() {
		    // given : 유효한 사용자 아이디, 존재하지 않는 상품 id 및 수량
			Long userId = testUser.getId();
			Long nonExistentProductId = 999L;
			Integer quantity = 1;

			// when & then
			ResourceNotFoundException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResourceNotFoundException.class, () -> {
					orderService.createOrder(userId, nonExistentProductId, quantity);
				});

			assertThat(exception.getMessage()).isEqualTo("Product not found with id " + nonExistentProductId);

		}

		@DisplayName("주문 생성 테스트 - 재고 부족")
		@Test
		void testCreateOrderWithInsufficientStock() {
		    // given : 유효한 사용자 Id, 상품 id, 및 재고보다 많은 수량
			Long userId = testUser.getId();
			Long productId = testProduct.getId();
			Integer quantity = 100; // 재고 50보다 큼


			// When & Then: 주문 생성 시 IllegalArgumentException이 발생하는지 검증
			IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
				orderService.createOrder(userId, productId, quantity);
			});

			assertThat(exception.getMessage()).isEqualTo("Insufficient stock for product id " + productId);

			// Then: 상품의 재고가 변경되지 않았는지 검증
			Product updatedProduct = productRepository.findById(productId).orElse(null);
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(50); // 재고 변동 없음

		}

		@DisplayName("주문 수량 업데이트 테스트 - 주문 상태가 PENDING 이 아님")
		@Test
		void testUpdateOrderQuantityNotPending() {
		    // given : 주문을 생성 및 저장 ( 서비스 메서드 사용 )
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

			// given : 주문 상태를 COMPLETED 로 변경
			order.setStatus(OrderStatus.COMPLETED);
			orderRepository.save(order);

			// when & then : 주문 수량 업데이트 시도 시 IllegalArgumentException 발생 검증
			IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
				IllegalArgumentException.class, () -> {
					orderService.updateOrderQuantity(order.getId(), 4);
				});

			assertThat(exception.getMessage()).isEqualTo("Only pending orders can be updated.");

			// then : 주문 수량이 변경되지 않았는지 검증
			Order updatedOrder = orderRepository.findById(order.getId()).orElse(null);
			assertThat(updatedOrder).isNotNull();
			assertThat(updatedOrder.getQuantity()).isEqualTo(2);
			assertThat(updatedOrder.getTotalAmount()).isEqualTo(200.0);

		}

		@DisplayName("주문 수량 업데이트 테스트 - 재고 부족")
		@Test
		void testUpdateOrderQuantityInsufficientStock() {
		    // given : 주문 생성 및 저장
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

			// when & then : 주문 수량을 재고 초과값으로 업데이트 시도 시 IllegalArgumentException 발생 여부 체크
			Integer newQuantity = 100;
			IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
				IllegalArgumentException.class, () -> {
					orderService.updateOrderQuantity(order.getId(), newQuantity);
				});

			assertThat(exception.getMessage()).isEqualTo("Insufficient stock to increase quantity.");

			// then : 주문 수량이 변경되지 않았는지 검증
			Order updatedOrder = orderRepository.findById(order.getId()).orElse(null);
			assertThat(updatedOrder).isNotNull();
			assertThat(updatedOrder.getQuantity()).isEqualTo(2);
			assertThat(updatedOrder.getTotalAmount()).isEqualTo(200.0);

			// then : 상품 재고가 변경되지 않았는지 검증
			Product updatedProduct = productRepository.findById(testProduct.getId()).orElse(null);
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(48);

		}


	}


}
