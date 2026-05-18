package vn.amela.employeeservice.service;

import vn.amela.employeeservice.dto.request.CreateEmployeeRequest;
import vn.amela.employeeservice.dto.request.EmployeeFilterRequest;
import vn.amela.employeeservice.dto.request.UpdateContactRequest;
import vn.amela.employeeservice.dto.request.UpdateEmployeeRequest;
import vn.amela.employeeservice.dto.response.EmployeeResponse;
import vn.amela.employeeservice.dto.response.PageResponse;

public interface EmployeeService {
    EmployeeResponse create(CreateEmployeeRequest request);

    EmployeeResponse getById(Long id, Long requesterId, String requesterRole);

    PageResponse<EmployeeResponse> search(EmployeeFilterRequest filter);

    EmployeeResponse updateByHr(Long id, UpdateEmployeeRequest request);

    EmployeeResponse updateContact(Long id, UpdateContactRequest request, Long requesterId);

    void deactivate(Long id);
}
