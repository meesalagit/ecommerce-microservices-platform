package com.surendra.userservice.service;

import com.surendra.userservice.dto.AdminUpdateUserDTO;
import com.surendra.userservice.dto.LoginRequestDTO;
import com.surendra.userservice.dto.LoginResponseDTO;
import com.surendra.userservice.dto.UpdateCurrentUserDTO;
import com.surendra.userservice.dto.UserResponseDTO;
import com.surendra.userservice.entity.Role;
import com.surendra.userservice.entity.User;
import com.surendra.userservice.exception.EmailAlreadyExistsException;
import com.surendra.userservice.exception.InvalidCredentialsException;
import com.surendra.userservice.exception.UserNotFoundException;
import com.surendra.userservice.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- helpers ----------

    private User user(Long id, String email, Role role) {
        User user = new User("Jane", "Doe", email, "encoded-password");
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private void loginAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of())
        );
    }

    // ---------- saveUser (register) ----------

    @Test
    void saveUser_newEmail_encodesPasswordAndAlwaysAssignsUserRole() {
        User newUser = new User("Jane", "Doe", "jane@test.com", "plain123");
        newUser.setRole(Role.ADMIN); // someone tries to sign up as ADMIN

        when(userRepository.existsByEmail("jane@test.com")).thenReturn(false);
        when(passwordEncoder.encode("plain123")).thenReturn("hashed123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserResponseDTO response = userService.saveUser(newUser);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed123");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("jane@test.com");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    void saveUser_duplicateEmail_throwsAndDoesNotSave() {
        User newUser = new User("Jane", "Doe", "jane@test.com", "plain123");
        when(userRepository.existsByEmail("jane@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.saveUser(newUser))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    // ---------- loginUser ----------

    @Test
    void loginUser_validCredentials_returnsTokenAndUser() {
        LoginRequestDTO request = new LoginRequestDTO();
        ReflectionTestUtils.setField(request, "email", "jane@test.com");
        ReflectionTestUtils.setField(request, "password", "plain123");

        User existing = user(1L, "jane@test.com", Role.USER);
        when(userRepository.findByEmail("jane@test.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("plain123", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken("jane@test.com")).thenReturn("jwt-token");

        LoginResponseDTO response = userService.loginUser(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo("jane@test.com");
    }

    @Test
    void loginUser_wrongPassword_throwsInvalidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO();
        ReflectionTestUtils.setField(request, "email", "jane@test.com");
        ReflectionTestUtils.setField(request, "password", "wrong");

        when(userRepository.findByEmail("jane@test.com"))
                .thenReturn(Optional.of(user(1L, "jane@test.com", Role.USER)));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.loginUser(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginUser_unknownEmail_throwsInvalidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO();
        ReflectionTestUtils.setField(request, "email", "nobody@test.com");
        ReflectionTestUtils.setField(request, "password", "plain123");

        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loginUser(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ---------- current user (/users/me) ----------

    @Test
    void getCurrentUser_returnsLoggedInUser() {
        loginAs("jane@test.com");
        when(userRepository.findByEmail("jane@test.com"))
                .thenReturn(Optional.of(user(1L, "jane@test.com", Role.ADMIN)));

        UserResponseDTO response = userService.getCurrentUser();

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void updateCurrentUser_emailTakenByAnotherUser_throws() {
        loginAs("jane@test.com");
        UpdateCurrentUserDTO update = new UpdateCurrentUserDTO();
        ReflectionTestUtils.setField(update, "email", "taken@test.com");

        when(userRepository.findByEmail("jane@test.com"))
                .thenReturn(Optional.of(user(1L, "jane@test.com", Role.USER)));
        when(userRepository.existsByEmailAndIdNot("taken@test.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateCurrentUser(update))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateCurrentUser_valid_updatesFieldsAndRehashesPassword() {
        loginAs("jane@test.com");
        UpdateCurrentUserDTO update = new UpdateCurrentUserDTO();
        ReflectionTestUtils.setField(update, "firstName", "Janet");
        ReflectionTestUtils.setField(update, "lastName", "Smith");
        ReflectionTestUtils.setField(update, "email", "janet@test.com");
        ReflectionTestUtils.setField(update, "password", "newPass1");

        User existing = user(1L, "jane@test.com", Role.USER);
        when(userRepository.findByEmail("jane@test.com")).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("janet@test.com", 1L)).thenReturn(false);
        when(passwordEncoder.encode("newPass1")).thenReturn("hashedNew");
        when(userRepository.save(existing)).thenReturn(existing);

        UserResponseDTO response = userService.updateCurrentUser(update);

        assertThat(response.getFirstName()).isEqualTo("Janet");
        assertThat(response.getEmail()).isEqualTo("janet@test.com");
        assertThat(existing.getPassword()).isEqualTo("hashedNew");
    }

    @Test
    void deleteCurrentUser_deletesLoggedInUser() {
        loginAs("jane@test.com");
        User existing = user(1L, "jane@test.com", Role.USER);
        when(userRepository.findByEmail("jane@test.com")).thenReturn(Optional.of(existing));

        userService.deleteCurrentUser();

        verify(userRepository).delete(existing);
    }

    // ---------- admin operations ----------

    @Test
    void getAllUsers_mapsEveryUserToDto() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "a@test.com", Role.USER),
                user(2L, "b@test.com", Role.ADMIN)
        ));

        List<UserResponseDTO> users = userService.getAllUsers();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(UserResponseDTO::getEmail)
                .containsExactly("a@test.com", "b@test.com");
    }

    @Test
    void getUserById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateUserByAdmin_canChangeRole() {
        AdminUpdateUserDTO update = new AdminUpdateUserDTO();
        ReflectionTestUtils.setField(update, "firstName", "Jane");
        ReflectionTestUtils.setField(update, "lastName", "Doe");
        ReflectionTestUtils.setField(update, "email", "jane@test.com");
        ReflectionTestUtils.setField(update, "role", Role.ADMIN);

        User existing = user(1L, "jane@test.com", Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("jane@test.com", 1L)).thenReturn(false);
        when(userRepository.save(existing)).thenReturn(existing);

        UserResponseDTO response = userService.updateUserByAdmin(1L, update);

        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void deleteUserByAdmin_notFound_throwsAndDoesNotDelete() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUserByAdmin(99L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).delete(any());
    }
}
