package com.kuzin.TelegamBotSpring;

import java.util.List;

public class ShoppingList {
    private Long userId;
    private List<String> shoppingList;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getShoppingList() {
        return shoppingList;
    }

    public void setShoppingList(List<String> shoppingList) {
        this.shoppingList = shoppingList;
    }
}
