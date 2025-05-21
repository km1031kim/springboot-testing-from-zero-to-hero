package io.github.junhkang.springboottesting.service.impl.kjg;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import io.github.junhkang.springboottesting.domain.Order;
import io.github.junhkang.springboottesting.domain.OrderDTO;
import io.github.junhkang.springboottesting.domain.OrderStatus;
import io.github.junhkang.springboottesting.domain.ProductDTO;
import io.github.junhkang.springboottesting.domain.UserDTO;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.mybatis.OrderMapper;
import io.github.junhkang.springboottesting.repository.mybatis.ProductMapper;
import io.github.junhkang.springboottesting.repository.mybatis.UserMapper;
import io.github.junhkang.springboottesting.service.impl.MyBatisOrderServiceImpl;

/**
 * 테스트 클래스 : MyBatisOrderServiceImplTest
 * 테스트 클래스 별 설명 기록..
 *
 * 해당 클래스는 MyBatisOrderServiceImpl 서비스 구현체의 비즈니스 로직 검증을 위한 단위 테스트 제공.
 *
 * 주요 특징 :
 * - MyBatis 는 SQL 쿼리를 직접 작성하고 매퍼를 통해 데이터베이스와 상호작용.
 * - 해당 테스트는 MyBatis 매퍼에서 작성된 SQL 쿼리가 의도한 대로 실행되고, 데이터베이스와의 상호작용이 올바른지 검증하는 것을 목표로 함.
 *
 *
 * 테스트 고려 사항 :
 * - SQL 쿼리가 기대한 결과를 반환하는지
 * - 매퍼 파일에서 정의된 쿼리와 객체 간 매핑이 올바르게 동작하는 지 검증
 * - 동적 SQL이 올바르게 생성되고 실행되는지
 * - 복잡한 SQL 쿼리가 동작하는 경우, 성능 상의 문제가 없는지
 *
 * @SpringBootTest 어노테이션으로 관련 빈 로드
 * @Import 를 통해 테스트 대한 클래스 로드
 * @ActiveProfile 설정을 통해 마이바티스 전용 프로파일 세팅
 */

@SpringBootTest
@Import(MyBatisOrderServiceImpl.class)
@ActiveProfiles("mybatis")
@Transactional
@DisplayName("MyBatisOrderServiceImplTest")
class MyBatisOrderServiceImplTest {

	@Autowired
	private OrderMapper orderMapper;

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private ProductMapper productMapper;

	@Autowired
	private MyBatisOrderServiceImpl orderService;

	// test prefix 붙이는 사내 규칙인 듯
	private UserDTO testUser;
	private ProductDTO testProduct;

	/**
	 * 테스트 전 데이터 초기화. @BeforeEach 사용
	 */

	@BeforeEach
	void setUp() {
		// Given: 테스트에 사용할 사용자 생성 및 저장
		testUser = new UserDTO();
		testUser.setUsername("test_user");
		testUser.setEmail("test.user@example.com");
		userMapper.insert(testUser); // insert 시 ID가 설정된다고 가정

		// Given: 테스트에 사용할 상품 생성 및 저장
		testProduct = new ProductDTO();
		testProduct.setName("Test Product");
		testProduct.setDescription("Test Description");
		testProduct.setPrice(100.0);
		testProduct.setStock(50);
		productMapper.insert(testProduct); // insert 시 ID가 설정된다고 가정
	}

	/**
	 * 조회 관련 테스트 그룹
	 */
	@Nested // 테스트 그룹 계층화. 중첩 클래스 정의
	@DisplayName("조회 관련 테스트")
	class RetrievalTests {

		@DisplayName("모든 주문 조회 테스트")
		@Test
		void testGetAllOrders() {
			// given
			// data.sql 과 아래 테스트 케이스를 종합하여 테스트
			orderService.createOrder(testUser.getId(), testProduct.getId(), 2);
			orderService.createOrder(testUser.getId(), testProduct.getId(), 1);

			// when
			List<Order> orders = orderService.getAllOrders();

			// then
			assertThat(orders).hasSize(7);
		}

		@DisplayName("주문ID로 주문 조회 테스트 - 존재하는 ID")
		@Test
		void testGetOrderByIdExists() {
			// given
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 3);

			// when : 존재하는 주문 ID로 주문 조회
			Order foundOrder = orderService.getOrderById(order.getId());

			// then : 주문이 정상적으로 조회되고, 세부 사항이 올바른지 검증
			assertThat(foundOrder).isNotNull();
			assertThat(foundOrder.getId()).isEqualTo(order.getId());
			assertThat(foundOrder.getUser().getUsername()).isEqualTo("test_user");
			assertThat(foundOrder.getProduct().getName()).isEqualTo("Test Product");
			assertThat(foundOrder.getQuantity()).isEqualTo(3);
			assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
			assertThat(foundOrder.getTotalAmount()).isEqualTo(300.0);
		}

		@DisplayName("주문ID 로 주문 조회 테스트 - 존재하지 않는 ID")
		@Test
		void testGetOrderByIdNotExists() {
			// given : 존재하지 않는 주문 ID
			Long nonExistentId = 999L;

			// when & then : 주문 조회 시 ResourceNotFoundException 발생 확인
			ResourceNotFoundException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResourceNotFoundException.class,
				() -> orderService.getOrderById(nonExistentId));

			assertThat(exception.getMessage()).isEqualTo("Order not found with id " + nonExistentId);
		}

		@DisplayName("사용자 ID로 주문 조회 테스트")
		@Test
		void testGetOrderByUserId() {
			// given : 다른 사용자 생성 및 저장
			UserDTO anotherUser = new UserDTO();
			anotherUser.setUsername("another_user");
			anotherUser.setEmail("another.user@example.com");
			userMapper.insert(anotherUser);

			// given : testUser의 주문 생성 및 저장
			Order order1 = orderService.createOrder(testUser.getId(), testProduct.getId(), 1);

			// given : atnoherUser의 주문 생성 및 저장
			Order order2 = orderService.createOrder(anotherUser.getId(), testProduct.getId(), 3);

			// when : testUser의 모든 주문을 조회
			List<Order> userOrders = orderService.getOrdersByUserId(testUser.getId());

			// then : testUser의 주문만 조회되었는지 검증
			assertThat(userOrders).hasSize(1);
			assertThat(userOrders.get(0).getUser().getUsername()).isEqualTo("test_user");
		}

		@DisplayName("주문 날짜 범위로 주문 조회 테스트")
		@Test
		void testGetOrdersByDateRange() {
			// given : 주문 생성 및 저장 ( 서비스 메서드 사용 )
			Order order1 = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);
			Order order2 = orderService.createOrder(testUser.getId(), testProduct.getId(), 1);

			// 날짜 설정
			LocalDateTime date1 = LocalDateTime.of(2023, 1, 1, 10, 0);
			LocalDateTime date2 = LocalDateTime.of(2023, 6, 15, 15, 30);
			OrderDTO savedOrder1 = orderMapper.findById(order1.getId());
			savedOrder1.setOrderDate(date1);
			orderMapper.update(savedOrder1);

			OrderDTO savedOrder2 = orderMapper.findById(order2.getId());
			savedOrder2.setOrderDate(date2);
			orderMapper.update(savedOrder2);

			// When: 특정 날짜 범위를 설정하여 주문을 조회
			LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
			LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);
			List<Order> ordersInRange = orderService.getOrdersByDateRange(startDate, endDate);

			// then : 검증
			assertThat(ordersInRange).hasSize(2);

			// then : orderDate 검증
			// 범위 내에 있는지
			assertThat(ordersInRange).allMatch(order ->
				!order.getOrderDate().isBefore(startDate) && !order.getOrderDate().isAfter(endDate));

			// 추가 검증 : 특정 주문이 포함되어 있는지 확인
			assertThat(ordersInRange)
				.extracting(Order::getId)
				.contains(savedOrder1.getId(), savedOrder2.getId());

		}

	}

	/**
	 * 생성 및 수정 관련 테스트 그룹
	 */

	@Nested
	@DisplayName("생성 및 수정 관련 테스트")
	class CreationAndUpdateTests {

		@DisplayName("주문 생성 테스트 - 성공 케이스")
		@Test
		void testCreateOrderSuccess() {
			// given : 유요한 사용자 ID, 상품 ID 및 수량
			Long userId = testUser.getId();
			Long productId = testProduct.getId();
			Integer quantity = 5;

			// when : 주문 생성
			Order createdOrder = orderService.createOrder(userId, productId, quantity);

			// then : 주문이 정상적으로 생성되고, 관련 데이터가 올바르게 업데이트되는지 검증

			assertThat(createdOrder).isNotNull();
			assertThat(createdOrder.getId()).isNotNull();
			assertThat(createdOrder.getUser().getId()).isEqualTo(userId);
			assertThat(createdOrder.getProduct().getId()).isEqualTo(productId);
			assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
			assertThat(createdOrder.getTotalAmount()).isEqualTo(testProduct.getPrice() * quantity);

			// then : 상품 재고 감소 검증
			ProductDTO updatedProduct = productMapper.findById(productId);
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(45);
		}

		@DisplayName("주문 생성 테스트 - 실패 케이스 (존재하지 않는 사용자 ID)")
		@Test
		void testCreateOrderWithNonExistentUser() {
			// given: 존재하지 않는 사용자 ID, 유효한 상품 ID 및 수량
			Long userId = testUser.getId();
			Long nonExistentProductId = 999L;

			Integer quantity = 1;

			// when & then : 주문 생성 시 ResourceNotFoundException 이 발생하는지 검증
			ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class, () -> {
				orderService.createOrder(userId, nonExistentProductId, quantity);
			});

			assertThat(exception.getMessage()).isEqualTo("Product not found with id " + nonExistentProductId);
		}

		@DisplayName("주문 생성 테스트 - 실패 케이스.. 재고 부족")
		@Test
		void testCreateOrderWithInsufficientStock() {
			// given : 유효한 사용자 ID, 상품 ID 및 재고보다 많은 수량
			Long userId = testUser.getId();
			Long produdctId = testProduct.getId();
			Integer quantity = 100; // 50보다 큼..

			// when & then
			IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
				() ->
					orderService.createOrder(userId, produdctId, quantity));

			assertThat(exception.getMessage()).isEqualTo("Insufficient stock for product id " + produdctId);

			// Then : 상품의 재고가 변경되지 않았는지 검증
			ProductDTO updatedProduct = productMapper.findById(produdctId);
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(50);
		}

		@DisplayName("주문 수량 업데이트 테스트 - 성공 케이스 ( 증가 )")
		@Test
		void testUpdateOrderQuantityIncrease() {
			// given
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

			// when : 주문 수량을 4로 증가
			Integer newQuantity = 4;
			Order updatedOrder = orderService.updateOrderQuantity(order.getId(), newQuantity);

			// then : 주문 수량과 총 금액이 올바르게 업데이트되었는지 검증
			assertThat(updatedOrder.getQuantity()).isEqualTo(newQuantity);
			assertThat(updatedOrder.getTotalAmount()).isEqualTo(testProduct.getPrice() * newQuantity);

			// then : 장품의 재고가 올바르게 감소했는지 검증
			ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(46);
		}

		@DisplayName("주문 수량 업데이트 테스트 - 실패 케이스 ( 재고 부족 )")
		@Test
		void testUpdateOrderQuantityInsufficientStock() {
			// given : 주문 생성 및 저장
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

			// when & then : 주문 수량을 재고를 초과하는 값으로 업데이트 시도 시 IllegalArgumentException 발생
			Integer newQuantity = 100;
			IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
				orderService.updateOrderQuantity(order.getId(), newQuantity);
			});

			assertThat(exception.getMessage()).isEqualTo("Insufficient stock to increase quantity.");

			// then : 주문 수량이 변경되지 않았는지
			OrderDTO updatedOrder = orderMapper.findById(order.getId());
			assertThat(updatedOrder).isNotNull();
			assertThat(updatedOrder.getQuantity()).isEqualTo(2);
			assertThat(updatedOrder.getTotalAmount()).isEqualTo(200.0);

			// then : 상품의 재고가 변경되지 않았는지
			ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(48);
		}

		@DisplayName("주문 수량 업데이트 테스트 - 실패 케이스 (주문 상태가 PENDING 이 아님")
		@Test
		void testUpdateOrderQuantityNotPending() {
			// given
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

			// 주문 상태 변경 : COMPLETED
			OrderDTO dto = orderMapper.findById(order.getId());
			dto.setStatus(OrderStatus.COMPLETED.name());
			orderMapper.update(dto);

			// when & then : 주문 수량 업데이트 시도 시 IllegalArgumentException 이 발생하는지 검증
			IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
				() -> orderService.updateOrderQuantity(order.getId(), 4));

			assertThat(exception.getMessage()).isEqualTo("Only pending orders can be updated.");

			// then : 이후 주문 수량이 변경되지 않았는지 검증
			OrderDTO updatedOrder = orderMapper.findById(order.getId());
			assertThat(updatedOrder).isNotNull();
			assertThat(updatedOrder.getQuantity()).isEqualTo(2);
			assertThat(updatedOrder.getTotalAmount()).isEqualTo(200.0);

			// then : 상품의 재고가 변경되지 않았는지 검증
			ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(48);
		}
	}

	/**
	 * 취소 관련 테스트 그룹
	 *
	 */
	@Nested
	@DisplayName("취소 관련 테스트 그룹")
	class CancellationTests {

		@DisplayName("주문 취소 테스트 - 성공 케이스")
		@Test
		void testCancelOrderSuccess() {
			// given : 주문 생성 및 저장
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

			// when : 주문 취소
			Order canceledOrder = orderService.cancelOrder(order.getId());

			// then : 주문 상태 -> CANCELED
			assertThat(canceledOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);

			// then : 상품의 재고가 복구되었는지 검증
			ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(50);
		}

		@DisplayName("주문 취소 테스트 - 실패 케이스 ( 주문 상태가 PENDING 이 아님 ")
		@Test
		void testCancelOrderNotPending() {
			// given : 주문을 생성 및 저장 ( 서비스 메서드 사용 )
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

			// given : 주문 상태를 COMPLETED 로 변경
			OrderDTO dto = orderMapper.findById(order.getId());
			dto.setStatus(OrderStatus.COMPLETED.name());
			orderMapper.update(dto);

			// when & then : 주문을 취소 시도 시 IllegalArgumentExcpetion 이 발생하는지 검증
			IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
				() -> orderService.cancelOrder(order.getId()));

			assertThat(exception.getMessage()).isEqualTo("Only pending orders can be canceled.");

			// then : 주문 상태가 변경되지 않았는지 검증
			// dto는 string으로 status 타입을 지정..
			OrderDTO updatedOrder = orderMapper.findById(order.getId());
			assertThat(updatedOrder).isNotNull();
			assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED.toString());

			// then : 상품 재고가 변경되지 않았는지 검증
			ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(48);

		}

	}

	/**
	 * 주문 금액 계산 관련 테스트
	 */

	@Nested
	@DisplayName("주문 금액 계산 관련 테스트")
	class CalculateTotalAmountTests {

		/**
		 * 주문 금액 계산 테스트
		 */

		@DisplayName("주문 금액 계산 테스트")
		@Test
		void testCalculateTotalAmount() {
			// given : 주문을 생성 및 저장
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 5);

			// when : 총 금액 계산
			Double totalAmount = orderService.calculateTotalAmount(order.getId());

			// then : 총 금액 검증
			assertThat(totalAmount).isEqualTo(500.0);
		}

	}

	/**
	 * 예외 상황 관련 테스트 그룹
	 */

	@Nested
	@DisplayName("예외 상황 관련 테스트")
	class ExceptionTests {

		@DisplayName("주문 생성 테스트 - 존재하지 않는 사용자 ID")
		@Test
		void testCreateOrderWithNonExistentUser() {
			// given: 존재하지 않는 사용자 ID, 유효한 상품 ID 및 수량
			Long nonExistentUserId = 999L;
			Long productId = testProduct.getId();
			Integer quantity = 1;

			// when
			ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
				() -> orderService.createOrder(nonExistentUserId, productId, quantity));

			// then
			assertThat(exception.getMessage()).isEqualTo("User not found with id " + nonExistentUserId);
		}

		@DisplayName("존재하지 않는 상품 ID")
		@Test
		void testCreateOrderWithNonExistentProduct() {
			// given : 유효한 사용자 ID, 존재하지 않는 상품 ID 및 수량
			Long userId = testUser.getId();
			Long nonExistentProductId = 999L;
			Integer quantity = 1;

			// when & then : 주문 생성 시 ResourceNotFoundExcpetion 이 발생하는지 검증
			ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
				() -> orderService.createOrder(userId, nonExistentProductId, quantity));

			assertThat(exception.getMessage()).isEqualTo("Product not found with id " + nonExistentProductId);
		}

		@DisplayName("주문 생성 테스트 - 재고 부족")
		@Test
		void testCreateOrderWithInsufficientStock() {
			// given : 유효한 사용자 ID, 상품 ID, 재고보다 많은 수량
			Long userId = testUser.getId();
			Long productId = testProduct.getId();
			Integer quantity = 100;

			// when & then : 주문 생성 시 IllegalArgumentExcpetion 발생 여부 검증
			IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
				() -> orderService.createOrder(userId, productId, quantity));

			assertThat(exception.getMessage()).isEqualTo("Insufficient stock for product id " + productId);

			// then : 상품의 재고가 변경되지 않았는지 검증
			ProductDTO updatedProduct = productMapper.findById(productId);
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(50); // 재고 변동 없음
		}

		@DisplayName("주문 취소 테스트 : 주문 상태가 PENDING 이 아님")
		@Test
		void testCancelOrderNotPending() {
			// given : 주문 생성 및 저장
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

			// given : 주문 상태를 COMPLETED 로 변경
			OrderDTO dto = orderMapper.findById(order.getId());
			dto.setStatus(OrderStatus.COMPLETED.name());
			orderMapper.update(dto);

			// when & then : 주문 취소 시도 시 IllegalArgumentException 발생 여부 검증
			IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
				() -> orderService.cancelOrder(order.getId()));

			// then : 주문 상태가 변경되지 않았는지 검증
			OrderDTO updatedOrder = orderMapper.findById(order.getId());
			assertThat(updatedOrder).isNotNull();
			assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED.toString());

			// then : 상품의 재고가 변경되지 않았는지 검증
			ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(48);

		}

		@DisplayName("주문 수량 업데이트 테스트 - 주문 상태가 PENDING 이 아님")
		@Test
		void testUpdateOrderQuantityNotPending() {
			// given : 주문 생성 및 저장
			Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

			// given : 주문 상태를 COMPLETED로 변경
			OrderDTO dto = orderMapper.findById(order.getId());
			dto.setStatus(OrderStatus.COMPLETED.name());
			orderMapper.update(dto);

			// when & then : 주문 수량 업데이트 시도 시 IllegalArugmentExcpetion 발생 여부 검증.. -> 상태 전용 예외를 만들면 어떨까..

			IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
				() -> orderService.updateOrderQuantity(order.getId(), 4));

			// then : 주문 수량이 변경되지 않았는지 검증
			OrderDTO updatedOrder
				= orderMapper.findById(order.getId());

			assertThat(updatedOrder).isNotNull();
			assertThat(updatedOrder.getQuantity()).isEqualTo(2);
			assertThat(updatedOrder.getTotalAmount()).isEqualTo(200.0);

			// then : 상품의 재고가 변경되지 않았는지 검증
			ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
			assertThat(updatedProduct).isNotNull();
			assertThat(updatedProduct.getStock()).isEqualTo(48);
		}

		@DisplayName("주문 수량 업데이트 테스트 - 존재하지 않는 주문 ID")
		@Test
		void testUpdateOrderQuantityNotExists() {
			// given : 존재하지 않는 주문 ID
			Long nonExistentOrderId = 999L;

			// when & Then : 주문 수량 업데이트 시 ResourceNotFoundException 발생 여부 검증
			ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
				() -> orderService.updateOrderQuantity(nonExistentOrderId, 5));

			assertThat(exception.getMessage()).isEqualTo("Order not found with id " + nonExistentOrderId);
		}

		@DisplayName("주문 취소 테스트 - 존재하지 않는 주문 ID")
		@Test
		void testCancelOrderNotExists() {
			// given : 존재하지 않는 주문 ID
			Long nonExistentOrderId = 999L;

			// when & then : 주문 취소 시 ResourceNotFoundException 발생 여부 검증
			ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
				() -> orderService.cancelOrder(nonExistentOrderId));

			assertThat(exception.getMessage()).isEqualTo("Order not found with id " + nonExistentOrderId);
		}

		@DisplayName("사용자 ID로 주문 조회 테스트 - 존재하지 않는 사용자 ID")
		@Test
		void testGetOrdersByUserIdNotExists() {
			// given : 존재하지 않는 사용자 ID
			Long nonExistentUserId = 999L;

			// when & then : 주문 조회 시 ResourceNotFoundException 발생 여부 검증
			ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
				() -> orderService.getOrdersByUserId(nonExistentUserId));

			assertThat(exception.getMessage()).isEqualTo("User not found with id " + nonExistentUserId);
		}

		@DisplayName("주문 금액 계산 테스트 - 존재하지 않는 주문 ID")
		@Test
		void testCalculateTotalAmountNotExists() {
			// given : 존재하지 않는 주문 ID
			Long nonExistentOrderId = 999L;

			// when & then : 총 금액 계산 시, ResourceNotFoundException 발생 여부 검증
			ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
				() -> orderService.calculateTotalAmount(nonExistentOrderId));

			assertThat(exception.getMessage()).isEqualTo("Order not found with id " + nonExistentOrderId);
		}

	}
}
