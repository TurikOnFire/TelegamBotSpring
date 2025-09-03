package com.kuzin.TelegamBotSpring.services;

import com.kuzin.TelegamBotSpring.entities.ShoppingList;
import com.kuzin.TelegamBotSpring.entities.User;
import com.kuzin.TelegamBotSpring.repositories.ShoppingListRepository;
import com.kuzin.TelegamBotSpring.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShoppingListService {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    public Optional<ShoppingList> getListById(Long id) {
        return shoppingListRepository.findById(id);
    }

    public Optional<ShoppingList> getListByUserId(Long userId) {
        return Optional.ofNullable(shoppingListRepository.findByUser_Id(userId));
    }

    public ShoppingList createList(ShoppingList shoppingList) {
        return shoppingListRepository.save(shoppingList);
    }
}
