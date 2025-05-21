package io.github.junhkang.springboottesting.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data // 교육용이니까 getter setter 사용 하신듯?
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime orderDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // 더블인게 좀 특이하다.
    private Double totalAmount;
}
