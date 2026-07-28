# Fraud-scoring evaluation harness

**Question this answers:** *how good is ClaimLens's fraud scoring, and how would we know?*

The fraud engine is a **rule-based, explainable risk *scorer*** (weighted rules → 0–100 →
LOW/MEDIUM/HIGH), not a trained classifier. You can't measure "accuracy" without ground truth, so this
harness makes the evaluation **methodology** concrete and repeatable.

## What it does

1. **Mirrors the real engine exactly** — the rules, weights and thresholds are copied from
   `backend/.../fraud/engine/FraudEngine.java` and `.../fraud/rule/*Rule.java`:

   | Rule | Weight | Fires when |
   |---|---|---|
   | `DUPLICATE_IMAGE` | 40 | a photo matches one on another claim |
   | `AMOUNT_OVER_SUM_INSURED` | 40 | claim amount > sum insured |
   | `SYNTHETIC_IMAGE` | 25 | a photo looks AI-generated |
   | `EARLY_CLAIM` | 20 | incident within 30 days of policy start |
   | `EXIF_INCONSISTENT` | 20 | photo metadata inconsistent with the incident |

   Thresholds: **MEDIUM ≥ 25**, **HIGH ≥ 50**.

2. **Generates a labelled synthetic dataset** (`is_fraud` 0/1) with **documented** per-rule fire
   probabilities (see `P_FIRE` in `fraud_eval.py`). Fraud is rare (~13–15% base rate) and the signals
   are noisy on purpose — some legit claims trip a rule benignly, some frauds are subtle — so the
   result is a *realistic, imperfect* classifier, not a trivially perfect one.

3. **Evaluates the score as a binary classifier** and computes, from scratch (no sklearn):
   - Confusion matrix + **precision / recall / F1 / specificity** at the HIGH and MEDIUM operating points
   - **ROC curve + AUC** (exact Mann-Whitney form) — threshold-independent ranking quality
   - **Precision-Recall curve + AUPRC** — the honest summary under class imbalance
   - **Per-rule lift** — for each rule, *of its firings what % were fraud* (precision) and *of all
     frauds what % it caught* (recall) → which rules to reweight or prune

Writes `report.json`.

## Run

```bash
# Synthetic dataset (documented assumptions) — the default:
python fraud_eval.py

# LIVE — evaluate on REAL investigator-labelled outcomes from a running app (the feedback loop):
python fraud_eval.py --live http://localhost:8080/api/v1 admin@demo.claimlens.app 'Password123!'

# A/B — compare two ruleset configs on the SAME labelled set (add --live to compare on real data):
python fraud_eval.py --compare config-default.json config-tuned.json
```

### A/B comparison (tuning a ruleset before you ship it)

A ruleset config is `{ "name", "weights": {RULE: weight}, "medium", "high" }` (a rule left out of
`weights` is disabled). `--compare` re-scores the same labelled claims under each config from their
recorded fired-rules and prints a side-by-side metrics diff. The included example acts on the
harness's own finding — demote the noisy `EARLY_CLAIM`, boost the sharp `DUPLICATE_IMAGE` — and it
measurably wins (AUPRC 0.66 → 0.68, HIGH F1 0.52 → 0.54). Tune offline against real outcomes, then
activate the winner on the Fraud Rulesets screen.

### The feedback loop (live mode)

Every claim decision now records a **ground-truth fraud label**: at *Decide*, the investigator ticks
"this claim was fraudulent" (approvals are recorded as not-fraud). The backend exposes the labelled
set at `GET /fraud/evaluation/dataset` (`FRAUD_READ`) — each decided claim's **score + fired rules +
true label**. `--live` logs in, pulls that dataset, and runs the *identical* metrics on real usage.

So the harness starts on transparent synthetic data and, as real decisions accumulate, the *same*
report runs on genuine outcomes — no code change, just more labels. (With only a handful of labelled
claims the numbers are noisy; it becomes meaningful as volume grows.)

## Why these metrics (not "accuracy")

Fraud is ~13% of claims here, so a model that flags **nothing** is ~87% "accurate" and useless. That's
why the harness leads with **precision/recall/AUPRC**: precision controls wasted investigator effort,
recall controls fraud leakage, and the MEDIUM/HIGH thresholds are a **business dial** you set from the
ROC/PR curve based on your tolerance for false alarms vs missed fraud.

## Honest caveats

- The labels are **synthetic**. This measures the *scoring logic* on transparent stand-in data, which
  is exactly the methodology you'd run on real **investigator-labelled outcomes** — swap the generator
  for real data and rerun. A portfolio project has no confirmed-fraud dataset; this is the honest way
  to demonstrate evaluation rather than fake real numbers.
- It's a **rule engine**, deliberately (insurance decisions must be auditable). Its ceiling is how well
  hand-tuned rules encode real fraud. The upgrade path, once enough real labels exist, is a supervised
  model (logistic regression / gradient-boosted trees) on the same features, cross-validated — at which
  point the same harness produces a rigorous accuracy story.
