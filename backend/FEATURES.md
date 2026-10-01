# DocTutorials CRM — Feature Roadmap (100+)

Legend: ✅ implemented in this codebase · 🔲 designed for, not yet coded (follows the same pattern described in README → "Adding a new feature")

## 1. Authentication & Access Control (12)
1. ✅ Email + password login issuing JWT
2. ✅ User registration with role assignment
3. ✅ Role-based access control (SUPER_ADMIN/ADMIN/MANAGER/COUNSELOR/VIEWER)
4. ✅ Method-level authorization (`@PreAuthorize`) on mutating endpoints
5. ✅ Stateless sessions (JWT, no server session state)
6. 🔲 Refresh tokens + silent re-auth
7. 🔲 Password reset via emailed one-time link
8. 🔲 Two-factor authentication (TOTP)
9. 🔲 Single Sign-On (Google/Microsoft OAuth2)
10. 🔲 Per-team data isolation (row-level security by team)
11. 🔲 Session/device management ("log out all devices")
12. 🔲 Login attempt throttling / account lockout

## 2. Lead / Pipeline Management (18)
13. ✅ Create/update/archive leads
14. ✅ Stage pipeline (New → Contacted → Qualified → Opportunity → Enrolled/Dropped)
15. ✅ Guard against moving leads out of a closed stage
16. ✅ Deterministic lead scoring (0–100)
17. ✅ Buying-intent derivation (Cold/Cool/Warm/Hot)
18. ✅ Convert-probability estimate
19. ✅ Stale-lead / at-risk detection ("inactive for N days")
20. ✅ Owner assignment & reassignment
21. ✅ Filter leads by stage / owner, paginated
22. 🔲 Duplicate-lead detection (phone/email fuzzy match) on create
23. 🔲 Bulk import from CSV with column mapping
24. 🔲 Bulk stage update / bulk reassignment
25. 🔲 Lead merge (combine two duplicate records)
26. 🔲 Custom fields per organization (schema-less extension)
27. 🔲 Lead scoring model versioning (A/B test scoring rules)
28. 🔲 Saved/shared lead views ("Hot pipeline", "New this week" from your screenshots)
29. 🔲 Kanban board view (drag-and-drop stage change)
30. 🔲 Lead-to-deal conversion (multiple deals per lead)

## 3. Tasks & Follow-ups (10)
31. ✅ Create tasks tied to a lead, with assignee/due date/priority
32. ✅ Mark task complete
33. ✅ Auto-flip to OVERDUE on a scheduled job
34. ✅ Task summary counts (pending/completed/overdue)
35. 🔲 Recurring tasks (daily/weekly follow-up cadences)
36. 🔲 Task templates per lead stage (auto-create "welcome call" on New)
37. 🔲 Task reminders (push/email/SMS N minutes before due)
38. 🔲 Task delegation with approval
39. 🔲 Subtasks / checklists within a task
40. 🔲 Calendar view of tasks across the team

## 4. Communication (Calls / WhatsApp / Email) (14)
41. ✅ Call logging (direction, status, duration, sentiment, recording URL)
42. ✅ Completed call refreshes lead's touchpoint + rescoring
43. ✅ Call summary metrics (total/completed/missed/avg duration)
44. 🔲 Click-to-call via telephony provider (Twilio/Exotel) integration
45. 🔲 Call recording playback + transcript storage
46. 🔲 AI call-sentiment auto-tagging from transcript
47. 🔲 WhatsApp message send/receive via WhatsApp Business API
48. 🔲 WhatsApp broadcast campaigns to a lead segment
49. 🔲 WhatsApp message templates (approved by Meta) library
50. 🔲 Email send via SMTP/SES with open/click tracking
51. 🔲 Email templates with merge fields
52. 🔲 Unified "Timeline" view merging calls+WhatsApp+email+notes chronologically
53. 🔲 SMS fallback channel
54. 🔲 In-app notification center

## 5. Notes, Documents, Contacts, Companies (10)
55. ✅ Free-text notes on a lead
56. 🔲 File/document upload & storage (S3-compatible) attached to a lead
57. 🔲 E-signature request + tracking on documents
58. 🔲 Related Contacts (multiple people per lead, e.g. parent + student)
59. 🔲 Related Companies (B2B mode: company account with multiple contacts)
60. 🔲 Invoice generation & tracking per lead
61. 🔲 Support ticket linkage (post-sale issues tied back to the original lead)
62. 🔲 @mention teammates inside notes with notification
63. 🔲 Note pinning / highlighting
64. 🔲 Rich-text notes with attachments

## 6. Automation & AI (12)
65. ✅ Rule-based "AI Reporting Assistant" over a fixed safe query set
66. ✅ Deterministic, explainable lead scoring (not a black box)
67. 🔲 Auto-generated AI lead summary (LLM-backed, replacing the manual `aiSummary` field)
68. 🔲 "Next Best Action" recommendation engine
69. 🔲 Automated stage-based email/WhatsApp drip sequences
70. 🔲 Workflow builder (trigger → condition → action, no-code)
71. 🔲 Auto-assignment rules (round-robin, by city, by course)
72. 🔲 SLA breach alerts (no contact within X hours of lead creation)
73. 🔲 Competitor-mention extraction from call/chat transcripts
74. 🔲 Churn-risk prediction for enrolled students (renewal use case)
75. 🔲 Duplicate-lead auto-merge suggestions
76. 🔲 Smart follow-up time suggestion (best time to call, per lead)

## 7. Reporting & Analytics (12)
77. ✅ Group leads by stage
78. ✅ Group leads by source
79. ✅ Counselor performance / conversion-rate leaderboard
80. ✅ Natural-language question → report routing
81. 🔲 Report Builder UI backend: arbitrary group-by + filter combos, saved reports
82. 🔲 Funnel/conversion visualization data endpoint
83. 🔲 Cohort analysis (leads by acquisition week vs conversion)
84. 🔲 Revenue/estimated-value forecasting
85. 🔲 Source ROI (spend vs conversions, needs a `campaigns.spend` field)
86. 🔲 Scheduled report email digests
87. 🔲 Export report to CSV/Excel/PDF
88. 🔲 Dashboard widgets API (configurable per user)

## 8. Admin, Teams & Governance (10)
89. ✅ Teams (grouping of users)
90. ✅ User activation/deactivation
91. ✅ Audit log (who did what, when) written on every mutation
92. 🔲 Per-entity audit history view ("Audit Logs" tab per lead)
93. 🔲 Org-level settings (business hours, holiday calendar, stale-lead threshold per org)
94. 🔲 Custom roles & granular permission matrix (beyond the 5 fixed roles)
95. 🔲 Data retention & GDPR-style "right to be forgotten" export/delete
96. 🔲 Webhook subscriptions for external integrations
97. 🔲 API keys for server-to-server integration (separate from user JWTs)
98. 🔲 Multi-tenant support (one deployment, many organizations)

## 9. Platform / Non-functional (8)
99. ✅ Optimistic locking (`@Version`) to prevent silent concurrent overwrites
100. ✅ Global exception handler → consistent JSON errors, no leaked stack traces
101. ✅ Bean-validated request DTOs (400 with field-level errors on bad input)
102. ✅ Paginated list endpoints everywhere (no unbounded "return all rows")
103. ✅ Flyway-versioned schema (no silent Hibernate auto-DDL drift)
104. 🔲 Rate limiting per API key/user
105. 🔲 Structured logging + request tracing (correlation IDs)
106. 🔲 Full CI pipeline: build → test → static analysis (SpotBugs/SonarQube) → deploy

---

**106 features listed, 32 implemented in this codebase, 74 scoped and ready to build** —
each new one following the exact same entity → repository → service → controller
pattern already established. Tell me which section to build out next and I'll
add it the same way.
