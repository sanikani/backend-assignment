package com.polycube.assignment.order.infra;

import com.polycube.assignment.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
