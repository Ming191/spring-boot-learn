package com.example.LibraryManagement.service;

import com.example.LibraryManagement.data.BookRepository;
import com.example.LibraryManagement.model.Book;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;

    @Transactional
    public Book borrow(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found: " + id));

        if (book.getIsBorrowed()) {
            throw new RuntimeException("Already borrowed");
        }

        book.setIsBorrowed(true);
        return book;
    }

    public List<Book> getAll() {
        return bookRepository.findAll();
    }

    public Book create(Book book) {
        bookRepository.save(book);
        return book;
    }
}