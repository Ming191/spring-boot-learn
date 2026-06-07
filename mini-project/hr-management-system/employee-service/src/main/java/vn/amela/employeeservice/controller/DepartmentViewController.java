package vn.amela.employeeservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import vn.amela.employeeservice.dto.request.CreateDepartmentRequest;
import vn.amela.employeeservice.dto.request.UpdateDepartmentRequest;
import vn.amela.employeeservice.dto.response.DepartmentResponse;
import vn.amela.employeeservice.service.DepartmentService;

@Controller
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentViewController {

    private final DepartmentService departmentService;

    @ModelAttribute("isHr")
    public boolean isHr(@RequestHeader(value = "X-Role", required = false) String userRole) {
        return "HR".equalsIgnoreCase(userRole);
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("departments", departmentService.listAll());
        return "departments/index";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("department", emptyCreateDepartmentRequest());
        return "departments/create";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("department") CreateDepartmentRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return "departments/create";
        }

        departmentService.create(request);
        return "redirect:/departments";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        DepartmentResponse department = departmentService.getById(id);
        UpdateDepartmentRequest form = UpdateDepartmentRequest.builder()
                .name(department.name())
                .description(department.description())
                .managerId(department.managerId())
                .isActive(department.isActive())
                .build();

        model.addAttribute("id", id);
        model.addAttribute("department", form);
        return "departments/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("department") UpdateDepartmentRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("id", id);
            return "departments/edit";
        }

        departmentService.update(id, request);
        return "redirect:/departments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        departmentService.delete(id);
        return "redirect:/departments";
    }

    private CreateDepartmentRequest emptyCreateDepartmentRequest() {
        return new CreateDepartmentRequest(null, null, null);
    }
}
