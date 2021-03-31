package com.example.demo.controller;

import com.example.demo.domain.QUser;
import com.example.demo.domain.User;
import com.example.demo.repository.UserRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.AllArgsConstructor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

@RestController
@AllArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserRepository userRepository;

    @PostConstruct
    public void createTestData() {
        userRepository.deleteAll();

        userRepository.save(User.builder().username("admin").password("admin").build());
        userRepository.save(User.builder().username("username").password("username").build());
    }

    @GetMapping("/{username}")
    public Iterable<User> users(@PathVariable(required = false) String username) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (!ObjectUtils.isEmpty(username)) {
            booleanBuilder.and(QUser.user.username.contains(username));
        }

        return userRepository.findAll(booleanBuilder);
    }

}
