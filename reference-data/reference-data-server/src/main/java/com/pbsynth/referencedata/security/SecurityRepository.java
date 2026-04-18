package com.pbsynth.referencedata.security;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SecurityRepository {

  private static final RowMapper<SecurityRecord> ROW =
      (ResultSet rs, int rowNum) ->
          new SecurityRecord(
              rs.getString("security_id"),
              rs.getString("ric"),
              rs.getString("isin"),
              rs.getString("currency"),
              rs.getString("asset_type"),
              rs.getString("description"),
              rs.getLong("version"));

  private final JdbcTemplate jdbc;

  public SecurityRepository(@Qualifier("securityJdbcTemplate") JdbcTemplate securityJdbcTemplate) {
    this.jdbc = securityJdbcTemplate;
  }

  public Optional<SecurityRecord> findById(String id) {
    List<SecurityRecord> list =
        jdbc.query("SELECT * FROM sec_security WHERE security_id = ?", ROW, id);
    return list.stream().findFirst();
  }

  public Optional<SecurityRecord> findByRic(String ric) {
    List<SecurityRecord> list =
        jdbc.query("SELECT * FROM sec_security WHERE ric = ?", ROW, ric);
    return list.stream().findFirst();
  }

  public List<SecurityRecord> findAllByIdIn(List<String> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    String placeholders = String.join(",", ids.stream().map(i -> "?").toList());
    return jdbc.query(
        "SELECT * FROM sec_security WHERE security_id IN (" + placeholders + ")", ROW, ids.toArray());
  }
}
