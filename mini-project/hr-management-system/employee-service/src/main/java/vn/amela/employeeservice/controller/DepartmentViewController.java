package vn.amela.employeeservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import vn.amela.employeeservice.dto.request.CreateDepartmentRequest;
import vn.amela.employeeservice.service.DepartmentService;

@Controller
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentViewController {

    private final DepartmentService departmentService;

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

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        departmentService.delete(id);
        return "redirect:/departments";
    }

    private CreateDepartmentRequest emptyCreateDepartmentRequest() {
        return new CreateDepartmentRequest(null, null, null);
    }
}
