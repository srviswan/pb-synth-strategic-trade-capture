package com.pbsynth.referencedata.book;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BookRepository {

  private static final RowMapper<BookRecord> ROW =
      (ResultSet rs, int rowNum) ->
          new BookRecord(
              rs.getString("book_id"),
              rs.getString("code"),
              rs.getString("entity_name"),
              rs.getString("desk"),
              rs.getString("normalised_key"),
              rs.getLong("version"));

  private final JdbcTemplate jdbc;

  public BookRepository(@Qualifier("bookJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<BookRecord> findById(String id) {
    List<BookRecord> list = jdbc.query("SELECT * FROM bk_book WHERE book_id = ?", ROW, id);
    return list.stream().findFirst();
  }

  public Optional<BookRecord> findByCode(String code) {
    List<BookRecord> list = jdbc.query("SELECT * FROM bk_book WHERE code = ?", ROW, code);
    return list.stream().findFirst();
  }

  public List<BookRecord> findAllByIdIn(List<String> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    String placeholders = String.join(",", ids.stream().map(i -> "?").toList());
    return jdbc.query(
        "SELECT * FROM bk_book WHERE book_id IN (" + placeholders + ")", ROW, ids.toArray());
  }
}
