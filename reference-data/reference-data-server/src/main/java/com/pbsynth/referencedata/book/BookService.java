package com.pbsynth.referencedata.book;

import com.pbsynth.referencedata.api.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BookService {

  private final BookRepository repository;

  public BookService(BookRepository repository) {
    this.repository = repository;
  }

  public BookRecord getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> new NotFoundException("BOOK_NOT_FOUND", "Book not found for id: " + id));
  }

  public BookRecord getByCode(String code) {
    return repository
        .findByCode(code)
        .orElseThrow(
            () -> new NotFoundException("BOOK_NOT_FOUND", "Book not found for code: " + code));
  }

  public List<BookRecord> batchByIds(List<String> ids) {
    return repository.findAllByIdIn(ids);
  }
}
