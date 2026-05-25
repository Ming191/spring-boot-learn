package vn.amela.employeeservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.amela.employeeservice.dto.request.CreateEmployeeRequest;
import vn.amela.employeeservice.dto.request.EmployeeFilterRequest;
import vn.amela.employeeservice.dto.request.UpdateContactRequest;
import vn.amela.employeeservice.dto.request.UpdateEmployeeRequest;
import vn.amela.employeeservice.dto.response.EmployeeResponse;
import vn.amela.employeeservice.dto.response.PageResponse;
import vn.amela.employeeservice.service.EmployeeService;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private static final String ROLE_HEADER = "X-Role";

    private final EmployeeService employeeService;

    @GetMapping
    public PageResponse<EmployeeResponse> getAllEmployees(
            @ModelAttribute EmployeeFilterRequest filter
    ) {
        return employeeService.search(filter);
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request
    ) {
        return ResponseEntity.ok(employeeService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(ROLE_HEADER) String userRole
    ) {
        return ResponseEntity.ok(employeeService.getById(id, userId, userRole));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        return ResponseEntity.ok(employeeService.updateByHr(id, request));
    }

    @PatchMapping("/{id}/contact")
    public ResponseEntity<EmployeeResponse> updateContact(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateContactRequest request
    ) {
        return ResponseEntity.ok(employeeService.updateContact(id, request, userId));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateEmployee(
            @PathVariable Long id
    ) {
        employeeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
