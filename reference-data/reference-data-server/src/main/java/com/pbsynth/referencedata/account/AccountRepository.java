package com.pbsynth.referencedata.account;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {

  private static final RowMapper<AccountRecord> ROW =
      (ResultSet rs, int rowNum) ->
          new AccountRecord(
              rs.getString("account_id"),
              rs.getString("name"),
              rs.getString("classification"),
              rs.getString("credit_tier"),
              rs.getBoolean("stp_eligible"),
              rs.getLong("version"));

  private final JdbcTemplate jdbc;

  public AccountRepository(@Qualifier("accountJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<AccountRecord> findById(String id) {
    List<AccountRecord> list =
        jdbc.query("SELECT * FROM acc_account WHERE account_id = ?", ROW, id);
    return list.stream().findFirst();
  }

  public List<AccountRecord> findAllByIdIn(List<String> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    String placeholders = String.join(",", ids.stream().map(i -> "?").toList());
    return jdbc.query(
        "SELECT * FROM acc_account WHERE account_id IN (" + placeholders + ")", ROW, ids.toArray());
  }

  /**
   * Upserts rows from an external feed. {@link AccountRecord#version()} on input is ignored;
   * inserts use version 1; updates increment the stored version.
   */
  public void upsertBatch(List<AccountRecord> records) {
    if (records.isEmpty()) {
      return;
    }
    String merge =
        """
        MERGE INTO acc_account AS t
        USING (
          VALUES (CAST(? AS VARCHAR(64)), CAST(? AS VARCHAR(256)), CAST(? AS VARCHAR(64)), CAST(? AS VARCHAR(32)), CAST(? AS BOOLEAN))
        ) AS s(account_id, name, classification, credit_tier, stp_eligible)
        ON t.account_id = s.account_id
        WHEN MATCHED THEN UPDATE SET
          name = s.name,
          classification = s.classification,
          credit_tier = s.credit_tier,
          stp_eligible = s.stp_eligible,
          version = t.version + 1
        WHEN NOT MATCHED THEN INSERT (
          account_id, name, classification, credit_tier, stp_eligible, version
        ) VALUES (
          s.account_id, s.name, s.classification, s.credit_tier, s.stp_eligible, 1
        )
        """;
    jdbc.batchUpdate(
        merge,
        records,
        records.size(),
        (ps, record) -> {
          ps.setString(1, record.accountId());
          ps.setString(2, record.name());
          ps.setString(3, record.classification());
          ps.setString(4, record.creditTier());
          ps.setBoolean(5, record.stpEligible());
        });
  }
}
