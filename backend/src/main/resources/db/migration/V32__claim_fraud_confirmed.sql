-- Ground-truth fraud label captured at decision time, so the fraud scorer can be evaluated on REAL
-- outcomes (not just synthetic data). NULL until the claim is decided:
--   approved            -> FALSE (paid, so not fraud)
--   rejected for fraud  -> TRUE
--   rejected otherwise  -> FALSE (denied for a coverage reason, still not fraud)
-- This is the label the evaluation harness joins against fraud_score to compute precision/recall/AUC
-- on live usage.
ALTER TABLE claim ADD COLUMN fraud_confirmed BOOLEAN;
