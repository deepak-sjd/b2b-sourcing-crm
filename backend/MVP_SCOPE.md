# MVP Scope — DocTutorials CRM (Client Delivery)

This defines exactly what "MVP" means for this delivery, so there's no
ambiguity about what's done, what's next, and why we're not building
everything in one pass.

## Why an MVP, not "all 106 features at once"

Three concrete reasons:

1. **Every feature needs to be individually correct and tested.** A CRM
   holding customer PII and revenue-relevant data can't afford a rushed
   WhatsApp integration shipped alongside 90 other half-checked features.
2. **You need something runnable to give feedback on early.** Better to
   validate the core lead pipeline works the way your client expects before
   building automation on top of it.
3. **Real production code takes real time per feature** — validation, error
   handling, tests, and audit logging for each module, not just a bare CRUD
   endpoint. That's what "not lazy/useless code" means in practice.

## MVP definition (what ships in this phase)

| Module | Included in MVP? | Why |
|---|---|---|
| Auth (JWT login/register) | ✅ Yes | Nothing works without it |
| Users & Roles | ✅ Yes | Every action needs an actor |
| Teams | ✅ Yes | Needed for ownership/reporting later |
| **Leads (full pipeline)** | ✅ Yes — **this is the product's core** | Scoring, stale-lead detection, stage machine |
| Tasks | ✅ Yes | Follow-up is the #1 thing that gets missed without a CRM |
| Calls | ✅ Yes | Basic touchpoint logging |
| Notes | ✅ Yes | Cheapest, highest-value communication record |
| Audit Log | ✅ Yes | Non-negotiable for a production client system |
| Reports (group-by + rule-based Q&A) | ✅ Yes, basic version | Client needs *some* visibility from day one |
| WhatsApp / Email integration | 🔲 Phase 2 | Needs a real provider account + webhook setup — a project on its own |
| Documents / Contacts / Companies | 🔲 Phase 2 | Not blocking for a working lead pipeline |
| Ollama AI Summary / Next Best Action | 🔲 Phase 3 | Needs the LLM infra decision finalized first |
| Automation / Workflow engine | 🔲 Phase 3 | Genuinely complex — deserves its own design pass |
| Full Report Builder UI backend | 🔲 Phase 4 | Current version covers the 80% case already |
| Multi-tenancy / SSO / Webhooks / Rate limiting | 🔲 Phase 5 | Only matters once there's more than one client org |

## Definition of "done" for each MVP module

Every module in the MVP must have, before we call it done:

- [ ] Entity with proper constraints (`@NotBlank`, indexes, foreign keys)
- [ ] Repository with only the queries actually used (no speculative methods)
- [ ] Service layer: business rules enforced, wrapped in `@Transactional`, writes to `AuditService`
- [ ] Controller: role-restricted via `@PreAuthorize`, returns the standard `ApiResponse<T>` envelope
- [ ] At least one integration test hitting the real endpoint (not just a unit test mocking everything)
- [ ] Flyway migration checked into `db/migration`, never relying on Hibernate auto-DDL
- [ ] Verified to actually compile and run locally (`./mvnw spring-boot:run` + a manual curl/Postman check)

## How we'll work through this together

1. **You set up the project in VS Code** (see `VSCODE_SETUP.md`) and confirm `./mvnw compile` succeeds
2. **We go module by module** — pick one from the table above, I build/fix it properly, you test it locally, we move to the next
3. **No module is "started" until the previous one compiles and runs clean**
4. Once the MVP table above is all ✅ and verified by you locally, we plan Phase 2 the same way

No fixed deadline, no rushing — correctness over speed, exactly as you asked.
