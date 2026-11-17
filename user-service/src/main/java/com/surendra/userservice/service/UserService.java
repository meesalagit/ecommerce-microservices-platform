package com.surendra.userservice.service;

import com.surendra.userservice.dto.AdminUpdateUserDTO;
import com.surendra.userservice.dto.LoginRequestDTO;
import com.surendra.userservice.dto.LoginResponseDTO;
import com.surendra.userservice.dto.UserResponseDTO;
import com.surendra.userservice.entity.Role;
import com.surendra.userservice.entity.User;
import com.surendra.userservice.exception.EmailAlreadyExistsException;
import com.surendra.userservice.exception.UserNotFoundException;
import com.surendra.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.surendra.userservice.exception.InvalidCredentialsException;
import java.util.List;
import com.surendra.userservice.dto.UpdateCurrentUserDTO;
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public UserResponseDTO saveUser(User user) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        user.setRole(Role.USER);

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        User savedUser = userRepository.save(user);

        return convertToDTO(savedUser);
    }

    public LoginResponseDTO loginUser(LoginRequestDTO loginRequest) {

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password")
                );

        boolean passwordMatches = passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword()
        );

        if (!passwordMatches) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail());

        UserResponseDTO userResponse = convertToDTO(user);

        return new LoginResponseDTO(token, userResponse);
    }

    public UserResponseDTO getCurrentUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        return convertToDTO(user);
    }

    public UserResponseDTO updateCurrentUser(UpdateCurrentUserDTO updatedUser) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User existingUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );
        if (userRepository.existsByEmailAndIdNot(
                updatedUser.getEmail(),
                existingUser.getId()
        )) {
            throw new EmailAlreadyExistsException("Email already exists");
        }


        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setEmail(updatedUser.getEmail());

        existingUser.setPassword(
                passwordEncoder.encode(updatedUser.getPassword())
        );

        User savedUser = userRepository.save(existingUser);

        return convertToDTO(savedUser);
    }

    public void deleteCurrentUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        userRepository.delete(user);
    }

    public List<UserResponseDTO> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public UserResponseDTO getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        return convertToDTO(user);
    }

    public UserResponseDTO updateUserByAdmin(
            Long id,
            AdminUpdateUserDTO updatedUser) {

        User existingUser = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );
        if (userRepository.existsByEmailAndIdNot(
                updatedUser.getEmail(),
                id
        )) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setRole(updatedUser.getRole());

        User savedUser = userRepository.save(existingUser);

        return convertToDTO(savedUser);
    }

    public void deleteUserByAdmin(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        userRepository.delete(user);
    }

    private UserResponseDTO convertToDTO(User user) {

        return new UserResponseDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole() != null
                        ? user.getRole().name()
                        : null
        );
    }
}