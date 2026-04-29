package com.example.LibraryManagement.controller;


import com.example.LibraryManagement.model.Book;
import com.example.LibraryManagement.service.BookService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/books")
public class BookController {
    private final BookService bookService;

    @Operation(summary = "Borrow a book", description = "Borrow a book if it is not already borrowed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book borrowed successfully"),
            @ApiResponse(responseCode = "404", description = "Book not found"),
            @ApiResponse(responseCode = "400", description = "Book already borrowed")
    })
    @PostMapping("/{id}/borrow")
    public ResponseEntity<EntityModel<Book>> borrow(@PathVariable Long id) {
        Book borrowedBook = bookService.borrow(id);
        return ResponseEntity.ok(EntityModel.of(borrowedBook));
    }

    @GetMapping
    public ResponseEntity<List<EntityModel<Book>>> getAll() {
        List<Book> books = bookService.getAll();

        List<EntityModel<Book>> result = books.stream()
                .map(EntityModel::of)
                .toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<EntityModel<Book>> create(@RequestBody Book book) {
        Book saved = bookService.create(book);
        return ResponseEntity.ok(EntityModel.of(saved));
    }
}
