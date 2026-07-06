package com.codingshuttle.youtube.hospitalManagement.service;

import com.codingshuttle.youtube.hospitalManagement.dto.DoctorResponseDto;
import com.codingshuttle.youtube.hospitalManagement.dto.onBoardNewRequestDto;
import com.codingshuttle.youtube.hospitalManagement.entity.Doctor;
import com.codingshuttle.youtube.hospitalManagement.entity.User;
import com.codingshuttle.youtube.hospitalManagement.entity.type.RoleType;
import com.codingshuttle.youtube.hospitalManagement.repository.DoctorRepository;
import com.codingshuttle.youtube.hospitalManagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {
    private final UserRepository userRepository;

    private final DoctorRepository doctorRepository;
    private final ModelMapper modelMapper;

    public List<DoctorResponseDto> getAllDoctors() {
        return doctorRepository.findAll()
                .stream()
                .map(doctor -> modelMapper.map(doctor, DoctorResponseDto.class))
                .collect(Collectors.toList());
    }


    @Transactional
    public DoctorResponseDto onBoardNewDoctor(onBoardNewRequestDto onboardnewrequestdto) {
     User user = userRepository.findById(onboardnewrequestdto.getUserId()).orElseThrow();

     if(doctorRepository.existsById(onboardnewrequestdto.getUserId())){
   throw new IllegalArgumentException("Doctor is Already Present");
     }
        Doctor doctor=Doctor.builder()
                .name(onboardnewrequestdto.getName())
                .specialization(onboardnewrequestdto.getSpecialization())
                .user(user)
                .build();
     user.getRoles().add(RoleType.DOCTOR);
     return modelMapper.map(doctorRepository.save(doctor), DoctorResponseDto.class);
    }
}
