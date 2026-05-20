package vn.amela.webservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import vn.amela.webservice.dto.request.CreateDepartmentForm;
import vn.amela.webservice.dto.request.UpdateDepartmentForm;
import vn.amela.webservice.dto.response.DepartmentResponse;

import java.util.List;

@FeignClient(name = "employee-service", contextId = "departmentClient", path = "/api/departments")
public interface DepartmentClient {

    @GetMapping
    List<DepartmentResponse> listAll();

    @PostMapping
    DepartmentResponse create(@RequestBody CreateDepartmentForm request);

    @PutMapping("/{id}")
    DepartmentResponse update(
        @PathVariable("id") Long id,
        @RequestBody UpdateDepartmentForm request
    );

    @DeleteMapping("/{id}")
    void delete(@PathVariable("id") Long id);
}
