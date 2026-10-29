# Class architecture and branding verification

Checked on 29 October 2026 against main source commit `48d55b49ecd9a7f21ba48b9c13d531469bb8cef4`.

| Check | Observed result |
|---|---|
| `bash gradlew clean build` | Passed; 220 tests, no failures/errors/skips; JaCoCo coverage gate passed |
| `npm --prefix frontend test` using Node 20.19.4 | Passed; 69 tests across 14 files |
| `npm --prefix frontend run build` | Passed with Vite 6.4.5 |
| `npm --prefix frontend audit` | Zero vulnerabilities after updating Vite, Vitest and the affected source-map dependency |
| `python3 docs/architecture/verify_class_diagrams.py` | Ten entities, eleven services/jobs, payment types, fourteen JPA associations, multiplicities and selected dependencies/members match source |
| draw.io exports | Both class views and their PDF visually inspected |
| Word excerpt | Three rendered pages inspected; two figures and captions; accessibility audit has zero findings |
| Original branding assets | All three copied PNGs match the team-provided originals by SHA-256 |
| Local browser | Landing, login and authenticated Browse verified at 1440px/390px widths; supplied images loaded; no horizontal overflow on tested mobile pages; zero browser console errors |

The local browser check used a temporary in-memory H2 database. The seeded buyer login reached Browse through the real API, and the app header displayed the primary logo. Landing navigation/footer used the yellow variant; login used the primary variant. The app icon is linked by favicon, Apple touch icon and manifest metadata.

Issues corrected: outdated event fields in the domain view; missing repository copies of the class views and source checker; placeholder branding; five reported frontend development-tool vulnerabilities; landing copy advertising unimplemented QR scanning/check-in; and documentation describing every object relation as many-to-one despite Ticket.seat being one-to-one.

This work covers the class-diagram part of the rubric. It does not update or assess the use case diagram. It does not add venue writes, server-side session enforcement, a QR check-in feature, or claim live Stripe/email validation. Those are distinct from the tested class documentation and branding flow.
