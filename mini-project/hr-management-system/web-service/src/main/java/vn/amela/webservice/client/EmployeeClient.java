package vn.amela.webservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import vn.amela.webservice.dto.request.CreateEmployeeForm;
import vn.amela.webservice.dto.request.EmployeeFilterRequest;
import vn.amela.webservice.dto.request.UpdateContactForm;
import vn.amela.webservice.dto.request.UpdateEmployeeForm;
import vn.amela.webservice.dto.response.EmployeeResponse;
import vn.amela.webservice.dto.response.PageResponse;

@FeignClient(name = "employee-service", contextId = "employeeClient", path = "/api/employees")
public interface EmployeeClient {

    String USER_ID_HEADER = "X-User-Id";
    String USER_ROLE_HEADER = "X-User-Role";

    @GetMapping
    PageResponse<EmployeeResponse> search(@SpringQueryMap EmployeeFilterRequest filter);

    @PostMapping
    EmployeeResponse create(@RequestBody CreateEmployeeForm request);

    @GetMapping("/{id}")
    EmployeeResponse getById(
        @PathVariable("id") Long id,
        @RequestHeader(USER_ID_HEADER) Long userId,
        @RequestHeader(USER_ROLE_HEADER) String userRole
    );

    @GetMapping("/by-auth-user/{authUserId}")
    EmployeeResponse getByAuthUserId(
        @PathVariable("authUserId") Long authUserId,
        @RequestHeader(USER_ID_HEADER) Long userId,
        @RequestHeader(USER_ROLE_HEADER) String userRole
    );

    @PutMapping("/{id}")
    EmployeeResponse update(
        @PathVariable("id") Long id,
        @RequestBody UpdateEmployeeForm request
    );

    @PatchMapping("/{id}/contact")
    EmployeeResponse updateContact(
        @PathVariable("id") Long id,
        @RequestHeader(USER_ID_HEADER) Long userId,
        @RequestBody UpdateContactForm request
    );

    @PatchMapping("/{id}/deactivate")
    void deactivate(@PathVariable("id") Long id);
}
