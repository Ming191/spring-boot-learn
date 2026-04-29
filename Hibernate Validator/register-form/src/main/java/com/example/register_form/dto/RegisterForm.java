package com.example.register_form.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterForm {
   @NotBlank(message = "Email cannot be blank")
   @Email(message = "Email must be valid")
    private String mail;

   @NotBlank(message = "Username cannot be blank")
   @Length(min = 3, max = 10, message = "Username must be between 3 and 10 characters")
    private String username;

   @NotBlank(message = "Password cannot be blank")
   @Pattern(regexp = "[a-zA-Z0-9]+", message = "Password must contain at least one letter and one number")
    private String password;

   @NotBlank(message = "Password cannot be blank")
   @Pattern(regexp = "[a-zA-Z0-9]+", message = "Password must contain at least one letter and one number")
    private String passwordConfirm;
}
