package vn.amela.employeeservice.service.impl;

import vn.amela.employeeservice.dto.request.CreateDepartmentRequest;
import vn.amela.employeeservice.dto.request.UpdateDepartmentRequest;
import vn.amela.employeeservice.dto.response.DepartmentResponse;
import vn.amela.employeeservice.service.DepartmentService;

import java.util.List;

public class DepartmentServiceImpl implements DepartmentService {
    @Override
    public DepartmentResponse create(CreateDepartmentRequest department) {
        return null;
    }

    @Override
    public List<DepartmentResponse> listAll() {
        return List.of();
    }

    @Override
    public List<DepartmentResponse> listAllActive() {
        return List.of();
    }

    @Override
    public DepartmentResponse update(Long id, UpdateDepartmentRequest request) {
        return null;
    }

    @Override
    public void deactivate(Long id) {

    }
}
