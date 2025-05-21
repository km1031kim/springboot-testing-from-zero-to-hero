package io.github.junhkang.springboottesting.controller.kjg;

import java.util.Collections;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.github.junhkang.springboottesting.controller.ProductController;
import io.github.junhkang.springboottesting.domain.Product;
import io.github.junhkang.springboottesting.service.ProductService;

@WebMvcTest(ProductController.class)
@DisplayName("ProductController 테스트")
class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ProductService productService;

	@DisplayName("모든 상품 조회 테스트")
	@Test
	void testGetAllProducts() throws Exception {
		// given
		Product product = new Product();
		product.setId(1L);
		product.setName("Test Product");
		Mockito.when(productService.getAllProducts()).thenReturn(Collections.singletonList(product));

		// when &  then
		mockMvc.perform(MockMvcRequestBuilders.get("/products"))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$[0].id", Matchers.is(1)))
			.andExpect(MockMvcResultMatchers.jsonPath("$[0].name", Matchers.is("Test Product")));

	}

	@DisplayName("상품 ID로 상품 조회 테스트")
	@Test
	void testGetProductById() throws Exception {
		// given
		Product product = new Product();
		product.setId(1L);
		product.setName("Test Product");
		Mockito.when(productService.getProductById(ArgumentMatchers.anyLong())).thenReturn(product);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.get("/products/1"))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(1)))
			.andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.is("Test Product")));
	}

	@DisplayName("상품 생성 테스트")
	@Test
	void testCreateProduct() throws Exception {
		// given
		Product product = new Product();
		product.setId(1L);
		product.setName("New Product");
		Mockito.when(productService.createProduct(ArgumentMatchers.any(Product.class))).thenReturn(product);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.post("/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"{\"name\": \"New Product\", \"description\": \"New Description\", \"price\" :  100.0, \"stock\" :  10}"))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(1)))
			.andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.is("New Product")));
	}

}
