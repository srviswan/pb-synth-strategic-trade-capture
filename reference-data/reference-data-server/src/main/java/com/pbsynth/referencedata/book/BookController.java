package com.pbsynth.referencedata.book;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {

  private final BookService service;

  public BookController(BookService service) {
    this.service = service;
  }

  @GetMapping("/{bookId}")
  public BookResponse getById(@PathVariable("bookId") String bookId) {
    return BookResponse.from(service.getById(bookId));
  }

  @GetMapping("/by-code")
  public BookResponse getByCode(@RequestParam("code") String code) {
    return BookResponse.from(service.getByCode(code));
  }

  @PostMapping("/batch")
  public Map<String, Object> batch(@RequestBody BatchBookRequest request) {
    List<BookResponse> books =
        service.batchByIds(request.ids()).stream().map(BookResponse::from).toList();
    return Map.of("books", books);
  }

  public record BatchBookRequest(List<String> ids) {}

  public record BookResponse(
      String bookId,
      String code,
      String entityName,
      String desk,
      String normalisedKey,
      long version) {

    static BookResponse from(BookRecord r) {
      return new BookResponse(
          r.bookId(), r.code(), r.entityName(), r.desk(), r.normalisedKey(), r.version());
    }
  }
}
