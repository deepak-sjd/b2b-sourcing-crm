-- V5: Generalize Lead away from coaching-specific naming, since this CRM is
-- now being built as a domain-agnostic platform, not a coaching-institute-only tool.
--
-- course_interested -> interest: same data, neutral name (works for a course,
-- a product, a service line - whatever this business sells).
--
-- stage 'ENROLLED'/'DROPPED' -> 'WON'/'LOST': standard sales-pipeline terminology.
-- A coaching-institute deployment can still show "Enrolled"/"Dropped" as the label
-- in its own UI - that's a display-layer mapping, not a stored value.

ALTER TABLE leads RENAME COLUMN course_interested TO interest;

UPDATE leads SET stage = 'WON' WHERE stage = 'ENROLLED';
UPDATE leads SET stage = 'LOST' WHERE stage = 'DROPPED';
