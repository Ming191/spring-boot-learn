package vn.amela.employeeservice.service.impl;

import org.springframework.stereotype.Service;
import vn.amela.employeeservice.dto.request.CreateEmployeeRequest;
import vn.amela.employeeservice.dto.request.EmployeeFilterRequest;
import vn.amela.employeeservice.dto.request.UpdateContactRequest;
import vn.amela.employeeservice.dto.request.UpdateEmployeeRequest;
import vn.amela.employeeservice.dto.response.EmployeeResponse;
import vn.amela.employeeservice.dto.response.PageResponse;
import vn.amela.employeeservice.service.EmployeeService;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Override
    public EmployeeResponse create(CreateEmployeeRequest request) {
        return null;
    }

    @Override
    public EmployeeResponse getById(Long id, Long requesterId, String requesterRole) {
        return null;
    }

    @Override
    public PageResponse<EmployeeResponse> search(EmployeeFilterRequest filter) {
        return null;
    }

    @Override
    public EmployeeResponse updateByHr(Long id, UpdateEmployeeRequest request) {
        return null;
    }

    @Override
    public EmployeeResponse updateContact(Long id, UpdateContactRequest request, Long requesterId) {
        return null;
    }

    @Override
    public void deactivate(Long id) {
    }
}
