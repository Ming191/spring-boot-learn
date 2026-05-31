package vn.amela.employeeservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import vn.amela.employeeservice.dto.request.CreateEmployeeRequest;
import vn.amela.employeeservice.dto.request.EmployeeFilterRequest;
import vn.amela.employeeservice.dto.request.UpdateContactRequest;
import vn.amela.employeeservice.dto.request.UpdateEmployeeRequest;
import vn.amela.employeeservice.dto.response.EmployeeResponse;
import vn.amela.employeeservice.dto.response.PageResponse;
import vn.amela.employeeservice.service.DepartmentService;
import vn.amela.employeeservice.service.EmployeeService;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeViewController {
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-Role";

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    @GetMapping
    public String index(@ModelAttribute EmployeeFilterRequest filter, Model model) {
        PageResponse<EmployeeResponse> page = employeeService.search(filter);

        model.addAttribute("page", page);
        model.addAttribute("employees", page.items());
        model.addAttribute("filter", filter);
        model.addAttribute("departments", departmentService.listAllActive());

        return "employees/index";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("employee", emptyCreateRequest());
        model.addAttribute("departments", departmentService.listAllActive());

        return "employees/create";
    }

    @PostMapping
    public String createEmployee(
            @Valid @ModelAttribute(value = "employee") CreateEmployeeRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentService.listAllActive());
            return "employees/create";
        }
        employeeService.create(request);
        return "redirect:/employees";
    }


    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestHeader(ROLE_HEADER) String userRole,
            Model model
    ) {
        EmployeeResponse employee = employeeService.getById(id, userId, userRole);

        UpdateEmployeeRequest form = new UpdateEmployeeRequest(
                employee.fullName(),
                employee.email(),
                employee.phone(),
                employee.position(),
                employee.departmentId(),
                employee.salary(),
                employee.startDate()
        );

        model.addAttribute("id", id);
        model.addAttribute("employee", form);
        model.addAttribute("departments", departmentService.listAllActive());

        return "employees/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("employee") UpdateEmployeeRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("departments", departmentService.listAllActive());
            return "employees/edit";
        }

        employeeService.updateByHr(id, request);
        return "redirect:/employees";
    }

    @GetMapping("/{id}/contact")
    public String contactForm(
            @PathVariable Long id,
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestHeader(ROLE_HEADER) String userRole,
            Model model
    ) {
        EmployeeResponse employee = employeeService.getById(id, userId, userRole);

        model.addAttribute("id", id);
        model.addAttribute("employeeName", employee.fullName());
        model.addAttribute("contact", new UpdateContactRequest(employee.email(), employee.phone()));

        return "employees/contact";
    }

    @PostMapping("/{id}/contact")
    public String updateContact(
            @PathVariable Long id,
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestHeader(ROLE_HEADER) String userRole,
            @Valid @ModelAttribute("contact") UpdateContactRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            EmployeeResponse employee = employeeService.getById(id, userId, userRole);
            model.addAttribute("id", id);
            model.addAttribute("employeeName", employee.fullName());
            return "employees/contact";
        }

        employeeService.updateContact(id, request, userId);
        return "redirect:/employees/" + id + "/contact";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id) {
        employeeService.deactivate(id);
        return "redirect:/employees";
    }

    private CreateEmployeeRequest emptyCreateRequest() {
        return new CreateEmployeeRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
