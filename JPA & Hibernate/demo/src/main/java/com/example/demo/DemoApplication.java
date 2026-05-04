package com.example.demo;

import com.example.demo.model.Student;
import com.example.demo.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner runner(StudentRepository studentRepository) {
        return args -> {
            System.out.println("Start CRUD demo");
            Student student = new Student("Nguyen Van B", "exampleB@gmail.com");
            studentRepository.save(student);
            System.out.println("Student saved");

            studentRepository.findAll().forEach(System.out::println);
            System.out.println("Student list");

            Optional<Student> studentOptional = studentRepository.findById(student.getId());
            studentOptional.ifPresent(s -> System.out.println("Student by id: " + s));

            Student foundStudent = studentRepository.findByEmail("example@gmail.com");
            System.out.println("Student by email: " + foundStudent);


            if (studentOptional.isPresent()) {
                Student studentToUpdate = studentOptional.get();

                studentToUpdate.setName("Nguyen Van A - VIP");

                studentRepository.save(studentToUpdate);

                System.out.println("Student sau khi update: " + studentRepository.findById(studentToUpdate.getId()).get());
            }

            studentRepository.delete(student);
            System.out.println("Student deleted");
            studentRepository.findAll().forEach(System.out::println);
            System.out.println("Student list");
            System.out.println("End CRUD demo");
        };
    }
}
