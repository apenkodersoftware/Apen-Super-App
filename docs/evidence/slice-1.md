# Slice 1 foundation evidence

Three tests passed: schema6-to7 migration (structural validation, original records and notes, unknown original entry times, exact earned/received/owed), transactional rejection of cross-business item use and overpayment, and SAT-S numeric part-payment/collection. See slice-1/ XML and Gradle log. Native screens and screenshots are pending; no full SAT marked Complete.

| Scenario | Status | Tests | Screenshots | Not verified |
|---|---|---|---|---|
| SAT-D | Not started | Script retained; not passed yet | None | Dairy workflow and restore |
| SAT-L | Not started | Script retained; not passed yet | None | Rental workflow and chooser |
| SAT-R | Not started | Script retained; not passed yet | None | Native workflow and timing |
| SAT-S | Partial | Exact numeric scenario, migration/isolation/rollback passed | R2 only | New shell and native acceptance |
| SAT-X | Partial | Migration and isolation subset passed | None | Full cross-cutting checks |
