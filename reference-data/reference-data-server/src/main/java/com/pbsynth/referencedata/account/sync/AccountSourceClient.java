package com.pbsynth.referencedata.account.sync;

import com.pbsynth.referencedata.account.AccountRecord;
import java.util.List;

/** Pulls account master rows from an external system (REST, file, etc.). */
public interface AccountSourceClient {

  List<AccountRecord> fetchAccounts();
}
