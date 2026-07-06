package com.codingshuttle.youtube.hospitalManagement.security;

import com.codingshuttle.youtube.hospitalManagement.dto.LoginRequestDto;
import com.codingshuttle.youtube.hospitalManagement.dto.LoginResponseDto;
import com.codingshuttle.youtube.hospitalManagement.dto.SignUpRequestDto;
import com.codingshuttle.youtube.hospitalManagement.dto.SignupResponseDto;
import com.codingshuttle.youtube.hospitalManagement.entity.Patient;
import com.codingshuttle.youtube.hospitalManagement.entity.User;
import com.codingshuttle.youtube.hospitalManagement.entity.type.AuthProviderType;
import com.codingshuttle.youtube.hospitalManagement.entity.type.RoleType;
import com.codingshuttle.youtube.hospitalManagement.repository.PatientRepository;
import com.codingshuttle.youtube.hospitalManagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

private final AuthenticationManager authenticationManager;
private final AuthUtil authUtil;
private final UserRepository userRepository;
private final PasswordEncoder passwordEncoder;
private final PatientRepository patientRepository;

    public LoginResponseDto login (LoginRequestDto loginRequestDto) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken
                        ( loginRequestDto.getUsername(),loginRequestDto.getPassword())
        );
        User user = (User) authentication.getPrincipal();

        String token = authUtil.generateAccessToken(user);

        return new LoginResponseDto(token,user.getId());
    }

    @Transactional
    public User signUpInternal(SignUpRequestDto signupRequestDto,AuthProviderType authProviderType, String providerId) {

        User user = userRepository.findByUsername(signupRequestDto.getUsername())
                .orElse(null);

        if (user != null)
            throw new IllegalArgumentException("User already exists");

        user = User.builder()
                .username(signupRequestDto.getUsername())
                .authProviderType(authProviderType)
                .providerId(providerId)
                .roles(signupRequestDto.getRoles())
                .build();

        if (authProviderType == AuthProviderType.EMAIL) {
            user.setPassword(passwordEncoder.encode(signupRequestDto.getPassword()));
        }

        user = userRepository.save(user);

        Patient patient = Patient.builder()
                .name(signupRequestDto.getName())
                .email(signupRequestDto.getUsername())
                .user(user)
                .build();
        patientRepository.save(patient);

        return user;
    }
    public SignupResponseDto signup(SignUpRequestDto signUpRequestDto) {
       User user = signUpInternal(signUpRequestDto,AuthProviderType.EMAIL,null);
        return new SignupResponseDto(user.getId(), user.getUsername());
    }

    @Transactional
    public ResponseEntity<LoginResponseDto> handleOAuth2LoginRequest(OAuth2User oAuth2User, String registrationId) {
        //Fetch the Provider-Id and Provider-type
        // Save the providerType and providerID withUser
        // If the user has an account directly login
        // Otherwise Signup and then login
        AuthProviderType authProviderType = authUtil.getProviderTypeFromRegistration(registrationId);
        String providerId = authUtil.determineProviderIdFromOAuth2User(oAuth2User, registrationId);
        User user =userRepository.findByProviderIdAndAuthProviderType(
                providerId,
                authProviderType
        ).orElse(null);
        String email = oAuth2User.getAttribute("email");
        User emailUser = userRepository.findByUsername(email).orElse(null);
        String name = oAuth2User.getAttribute("name");

        if (user == null && emailUser == null) {
            String username = authUtil.determineUsernameFromOAuth2User(oAuth2User, registrationId, providerId);
            user = signUpInternal(new SignUpRequestDto(username,null,name,Set.of(RoleType.PATIENT)),authProviderType, providerId);
        } else if (user != null) {
            if (email != null && !email.isBlank() && !email.equals(user.getUsername())) {
                user.setUsername(email);
                userRepository.save(user);
            }

        } else {
            user=emailUser;
            user.setProviderId(providerId);
            user.setAuthProviderType(authProviderType);
            userRepository.save(user);

        }
        LoginResponseDto loginResponseDto =
                new LoginResponseDto(authUtil.generateAccessToken(user), user.getId());
        return ResponseEntity.ok(loginResponseDto);
    }


}
