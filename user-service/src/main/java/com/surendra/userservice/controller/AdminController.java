package com.surendra.userservice.controller;

import com.surendra.userservice.dto.UserResponseDTO;
import com.surendra.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import com.surendra.userservice.dto.AdminUpdateUserDTO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public List<UserResponseDTO> getAllUsers() {
        return userService.getAllUsers();
    }
    @GetMapping("/users/{id}")
    public UserResponseDTO getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    @PutMapping("/users/{id}")
    public UserResponseDTO updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserDTO updatedUser) {

        return userService.updateUserByAdmin(id, updatedUser);
    }
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        userService.deleteUserByAdmin(id);

        return ResponseEntity.noContent().build();
    }
}