package com.systemdesign.freshcart.inventoryconsumer.stock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockItemRepository extends JpaRepository<StockItem, String> {

    List<StockItem> findAllByOrderBySkuAsc();
}
