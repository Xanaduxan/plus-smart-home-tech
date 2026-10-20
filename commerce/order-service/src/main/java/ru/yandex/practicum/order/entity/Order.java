package ru.yandex.practicum.order.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false, columnDefinition = "text")
    private String customerName;

    @Column(name = "customer_email", nullable = false, columnDefinition = "text")
    private String customerEmail;

    @Column(nullable = false)
    private String status = "CREATED";

    @Column(name = "total_price", nullable = false, columnDefinition = "numeric")
    private BigDecimal totalPrice;

    @Column(name = "status_details", columnDefinition = "text")
    private String statusDetails;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt =
            LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}