package com.surendra.userservice.controller;

import com.surendra.userservice.dto.LoginRequestDTO;
import com.surendra.userservice.dto.LoginResponseDTO;
import com.surendra.userservice.dto.UpdateCurrentUserDTO;
import com.surendra.userservice.dto.UserResponseDTO;
import com.surendra.userservice.entity.User;
import com.surendra.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public UserResponseDTO createUser(
            @Valid @RequestBody User user
    ) {
        return userService.saveUser(user);
    }

    @GetMapping("/{id}")
    public UserResponseDTO getUserById(
            @PathVariable Long id
    ) {
        return userService.getUserById(id);
    }

    @GetMapping("/me")
    public UserResponseDTO getCurrentUser() {
        return userService.getCurrentUser();
    }

    @PutMapping("/me")
    public UserResponseDTO updateCurrentUser(
            @Valid @RequestBody UpdateCurrentUserDTO updatedUser
    ) {
        return userService.updateCurrentUser(updatedUser);
    }

    @DeleteMapping("/me")
    public void deleteCurrentUser() {
        userService.deleteCurrentUser();
    }

    @PostMapping("/login")
    public LoginResponseDTO loginUser(
            @Valid @RequestBody LoginRequestDTO loginRequest
    ) {
        return userService.loginUser(loginRequest);
    }
}