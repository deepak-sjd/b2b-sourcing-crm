-- V7: Support for the Lead AI Insight panel (aiSummary already existed from V1;
-- this adds the companion "next best action" — a single, short, actionable
-- recommendation shown as a call-to-action chip, separate from the longer
-- free-text summary so the UI can render it distinctly.

ALTER TABLE leads ADD COLUMN next_best_action VARCHAR(300);
