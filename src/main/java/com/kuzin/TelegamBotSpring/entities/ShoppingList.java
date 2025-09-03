package com.kuzin.TelegamBotSpring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "t_shopping_list")
public class ShoppingList {

    @Id
    private Long id;


    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "shopping_list", nullable = false, length = Integer.MAX_VALUE)
    private String shoppingList;

}
