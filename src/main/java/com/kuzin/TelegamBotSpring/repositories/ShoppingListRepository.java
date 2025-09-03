package com.kuzin.TelegamBotSpring.repositories;

import com.kuzin.TelegamBotSpring.entities.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {
    ShoppingList findByUser_Id(Long id);
}