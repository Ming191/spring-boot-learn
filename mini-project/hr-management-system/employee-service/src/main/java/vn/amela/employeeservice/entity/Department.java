package vn.amela.employeeservice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Department {

    private Long id;

    private String name;

    private String description;

    private Long managerId;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
