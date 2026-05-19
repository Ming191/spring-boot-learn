package vn.amela.employeeservice.service;

import vn.amela.employeeservice.dto.request.CreateDepartmentRequest;
import vn.amela.employeeservice.dto.request.UpdateDepartmentRequest;
import vn.amela.employeeservice.dto.response.DepartmentResponse;

import java.util.List;

public interface DepartmentService {
    DepartmentResponse create(CreateDepartmentRequest department);
    List<DepartmentResponse> listAll();
    List<DepartmentResponse> listAllActive();
    DepartmentResponse update(Long id, UpdateDepartmentRequest request);
    void deactivate(Long id);
}
