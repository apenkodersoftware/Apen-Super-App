# Apen Super App - R3 review

One offline Android business-records app for dairy farmers, small landlords, food businesses and shopkeepers, with separate business profiles, English/Kiswahili screens, accounts, costs, history, backup/recovery and PDF/CSV statements.

**Owner-review build; not approved for customer distribution. Poultry is planned and is not included in R3.**

- [Download the tested APK and review files](https://github.com/apenkodersoftware/Apen-Super-App/releases/tag/r3-review-2026-10-29)
- [Product summary](docs/R3-PRODUCT-SUMMARY.md)
- [Acceptance results and handover](docs/R3-HANDOVER.md)
- [Build and publication notes](docs/PUBLICATION.md)
- [Roadmap](docs/PLAN.md)
- [Poultry scenario plan](docs/POULTRY-NEXT-ITERATION.md)
- [Test evidence](docs/evidence)

The reviewed APK is version 0.3.0-super-review-r3 (code 3), package com.apenkoder.apen.review, database schema 8. Its SHA-256 is fb5827f14160d54b7f005b20cc42d0e9b67ee9291554db44987ce19f18f33cf3.

The 29 October local review passed 188 JVM tests and the documented native acceptance scripts on emulator-5554. Lint had zero errors and 98 warnings. Physical-phone/outdoor and human Kiswahili review, production signing and distribution acceptance remain pending. GitHub Actions status is separate from these local results.

The app records user-entered activity. It does not confirm M-Pesa payments, submit or approve loans, provide an Inuka account balance, calculate full profit, score creditworthiness or prove that a transaction happened. Statements disclose recording coverage and omitted private party details.

No signing keys or private customer/recovery files are included. Follow the build notes to generate an ignored local review key. The release APK remains the exact previously tested binary.
