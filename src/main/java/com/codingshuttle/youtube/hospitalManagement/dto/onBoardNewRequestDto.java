package com.codingshuttle.youtube.hospitalManagement.dto;

import lombok.Data;

@Data
public class onBoardNewRequestDto {
    private Long userId;
    private String specialization;
    private String name;
}
