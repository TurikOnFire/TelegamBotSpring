package com.kuzin.TelegamBotSpring.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "t_employee")
public class User {

    @lombok.Setter
    @lombok.Getter
    @Id
    private Long id;

    @lombok.Setter
    @lombok.Getter
    @Column(name = "first_name", length = Integer.MAX_VALUE)
    private String firstName;

    @lombok.Setter
    @lombok.Getter
    @Column(name = "username", nullable = false, length = Integer.MAX_VALUE)
    private String username;

    @lombok.Setter
    @lombok.Getter
    @Column(name = "age")
    private Integer age;

}
