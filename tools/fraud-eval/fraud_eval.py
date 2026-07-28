#!/usr/bin/env python3
"""
ClaimLens — Fraud-scoring evaluation harness.

The fraud engine is a rule-based, explainable RISK SCORER (not a trained ML classifier). You cannot
measure "accuracy" without ground-truth labels, so this harness:

  1. Mirrors the EXACT rules/weights/thresholds from the real engine
     (backend .../fraud/engine/FraudEngine.java + .../fraud/rule/*Rule.java).
  2. Generates a labelled SYNTHETIC dataset — each claim tagged is_fraud=0/1 — with documented,
     defensible feature probabilities (a real portfolio has no confirmed-fraud dataset, so this is
     transparent stand-in data, not real claims).
  3. Scores every claim through the identical logic and evaluates it as a binary classifier at the
     engine's operating points, computing precision / recall / F1, confusion matrices, ROC + AUC,
     PR + AUPRC, and PER-RULE lift.
  4. Writes report.json (consumed by the visual report).

Honest framing: this measures the SCORING LOGIC on synthetic labels. The methodology is exactly what
you'd run on real investigator-labelled outcomes — swap the generator for real data and rerun.
"""
import argparse
import json
import os
import random
import urllib.request

# ── 1. The engine, mirrored (source of truth: FraudEngine.java default path) ──────────────────────
# Rule code -> (weight, human label). Weights are FraudEngine's DEFAULT_* constants.
RULES = {
    "AMOUNT_OVER_SUM_INSURED": (40, "Claim amount exceeds the sum insured"),
    "EARLY_CLAIM":             (20, "Claim within 30 days of policy start"),
    "DUPLICATE_IMAGE":         (40, "Photo reused from another claim"),
    "SYNTHETIC_IMAGE":         (25, "Photo appears AI-generated / synthetic"),
    "EXIF_INCONSISTENT":       (20, "Photo metadata inconsistent with incident"),
}
MEDIUM_THRESHOLD = 25
HIGH_THRESHOLD = 50


def score_claim(features):
    """Sum the weights of every rule whose feature fired — identical to FraudEngine.evaluate()."""
    fired = [code for code in RULES if features.get(code)]
    score = min(sum(RULES[c][0] for c in fired), 100)  # engine has no hard cap; we clamp for display
    risk = "HIGH" if score >= HIGH_THRESHOLD else "MEDIUM" if score >= MEDIUM_THRESHOLD else "LOW"
    return score, risk, fired


# ── 2. Synthetic labelled dataset with DOCUMENTED assumptions ──────────────────────────────────────
# P(rule fires | label). Grounded in motor-fraud patterns: fraud claims skew toward image reuse,
# early claims, and inflated amounts, but MANY frauds are subtle (one weak signal) and SOME legit
# claims trip a rule benignly (a genuine early claim, a stripped/clock-skewed EXIF, a heuristic
# synthetic false-positive). That noise is deliberate — it's what makes the evaluation meaningful
# instead of trivially perfect.
FRAUD_RATE = 0.15  # base rate — fraud is rare (class imbalance is the whole challenge)
P_FIRE = {
    #                     legit   fraud
    "AMOUNT_OVER_SUM_INSURED": (0.04, 0.35),
    "EARLY_CLAIM":             (0.12, 0.45),
    "DUPLICATE_IMAGE":         (0.01, 0.40),
    "SYNTHETIC_IMAGE":         (0.02, 0.15),
    "EXIF_INCONSISTENT":       (0.05, 0.30),
}
N_CLAIMS = 400
SEED = 42


def generate_dataset():
    rng = random.Random(SEED)
    claims = []
    for i in range(N_CLAIMS):
        is_fraud = 1 if rng.random() < FRAUD_RATE else 0
        features = {code: (rng.random() < P_FIRE[code][is_fraud]) for code in RULES}
        score, risk, fired = score_claim(features)
        claims.append({
            "id": i + 1, "is_fraud": is_fraud,
            "features": {c: int(features[c]) for c in RULES},
            "fired": fired, "score": score, "risk": risk,
        })
    return claims


# ── LIVE mode: pull REAL labelled outcomes from the running app (the feedback loop) ────────────────
def _api(url, method="GET", body=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def fetch_live(base_url, email, password):
    """Log in, pull GET /fraud/evaluation/dataset, and map real rows into the harness's claim shape."""
    base = base_url.rstrip("/")
    tok = _api(f"{base}/auth/login", "POST", {"email": email, "password": password})["data"]["accessToken"]
    rows = _api(f"{base}/fraud/evaluation/dataset", token=tok)["data"]
    claims = []
    for r in rows:
        claims.append({
            "id": r["claimId"], "is_fraud": 1 if r["fraudConfirmed"] else 0,
            "features": {c: int(c in r["firedRules"]) for c in RULES},
            "fired": r["firedRules"], "score": r["score"], "risk": r["riskLevel"],
        })
    return claims


# ── 3. Metrics (computed from scratch — no sklearn dependency) ─────────────────────────────────────
def confusion(claims, threshold):
    tp = fp = tn = fn = 0
    for c in claims:
        pred = c["score"] >= threshold
        if c["is_fraud"] and pred: tp += 1
        elif c["is_fraud"] and not pred: fn += 1
        elif not c["is_fraud"] and pred: fp += 1
        else: tn += 1
    return tp, fp, tn, fn


def rates(tp, fp, tn, fn):
    precision = tp / (tp + fp) if (tp + fp) else 0.0
    recall = tp / (tp + fn) if (tp + fn) else 0.0          # sensitivity / TPR
    specificity = tn / (tn + fp) if (tn + fp) else 0.0
    f1 = 2 * precision * recall / (precision + recall) if (precision + recall) else 0.0
    accuracy = (tp + tn) / (tp + fp + tn + fn)
    fpr = fp / (fp + tn) if (fp + tn) else 0.0
    return {"precision": precision, "recall": recall, "specificity": specificity,
            "f1": f1, "accuracy": accuracy, "fpr": fpr,
            "tp": tp, "fp": fp, "tn": tn, "fn": fn}


def roc_auc_mann_whitney(claims):
    """Exact AUC = P(score(fraud) > score(legit)) + 0.5*P(tie) — the Mann-Whitney U form."""
    frauds = [c["score"] for c in claims if c["is_fraud"]]
    legits = [c["score"] for c in claims if not c["is_fraud"]]
    if not frauds or not legits:
        return 0.0
    wins = ties = 0
    for f in frauds:
        for l in legits:
            if f > l: wins += 1
            elif f == l: ties += 1
    return (wins + 0.5 * ties) / (len(frauds) * len(legits))


def roc_curve(claims):
    thresholds = sorted({c["score"] for c in claims} | {0, 101}, reverse=True)
    pts = []
    for t in thresholds:
        r = rates(*confusion(claims, t))
        pts.append({"threshold": t, "fpr": round(r["fpr"], 4), "tpr": round(r["recall"], 4)})
    # ensure endpoints (0,0) and (1,1)
    pts = sorted(pts, key=lambda p: (p["fpr"], p["tpr"]))
    return pts


def pr_curve_and_auprc(claims):
    thresholds = sorted({c["score"] for c in claims} | {1}, reverse=False)
    pts = []
    for t in thresholds:
        r = rates(*confusion(claims, t))
        pts.append({"threshold": t, "recall": round(r["recall"], 4), "precision": round(r["precision"], 4)})
    # AUPRC via trapezoid over recall (sort by recall ascending)
    s = sorted(pts, key=lambda p: p["recall"])
    auprc = 0.0
    for a, b in zip(s, s[1:]):
        auprc += (b["recall"] - a["recall"]) * (a["precision"] + b["precision"]) / 2
    return pts, auprc


def per_rule_lift(claims):
    frauds_total = sum(c["is_fraud"] for c in claims)
    out = []
    for code, (weight, label) in RULES.items():
        fired = [c for c in claims if code in c["fired"]]
        fired_fraud = sum(c["is_fraud"] for c in fired)
        out.append({
            "code": code, "label": label, "weight": weight,
            "fired": len(fired),
            "precision": round(fired_fraud / len(fired), 4) if fired else 0.0,  # of firings, % fraud
            "recall": round(fired_fraud / frauds_total, 4) if frauds_total else 0.0,  # of frauds, % caught
        })
    return sorted(out, key=lambda r: -r["precision"])


# ── A/B: re-score the SAME labelled claims under a candidate ruleset config, compare metrics ────────
def evaluate_config(claims, weights, medium, high):
    """Re-score each claim from its fired rules under a candidate weight/threshold config."""
    rescored = [{**c, "score": min(sum(weights.get(r, 0) for r in c["fired"]), 100)} for c in claims]
    _, auprc = pr_curve_and_auprc(rescored)
    return {
        "auc": round(roc_auc_mann_whitney(rescored), 4), "auprc": round(auprc, 4),
        "high_t": high, "medium_t": medium,
        "high": rates(*confusion(rescored, high)),
        "medium": rates(*confusion(rescored, medium)),
    }


def compare_configs(claims, cfg_a, cfg_b):
    """Print a side-by-side metrics diff for two ruleset configs on the identical labelled set."""
    a = evaluate_config(claims, cfg_a["weights"], cfg_a.get("medium", 25), cfg_a.get("high", 50))
    b = evaluate_config(claims, cfg_b["weights"], cfg_b.get("medium", 25), cfg_b.get("high", 50))

    def delta(x, y):
        d = y - x
        return f"{d:+.3f}" if abs(d) >= 0.001 else "   --"

    na, nb = cfg_a.get("name", "A")[:30], cfg_b.get("name", "B")[:30]
    print(f"\nComparing on {len(claims)} labelled claims "
          f"({sum(c['is_fraud'] for c in claims)} fraud):\n")
    print(f"{'':24}{'A: ' + na:>32}{'B: ' + nb:>32}     chg")
    print("-" * 100)
    rows = [
        ("ROC AUC",           a["auc"], b["auc"]),
        ("AUPRC",             a["auprc"], b["auprc"]),
        (f"HIGH  precision",  a["high"]["precision"], b["high"]["precision"]),
        (f"HIGH  recall",     a["high"]["recall"], b["high"]["recall"]),
        (f"HIGH  F1",         a["high"]["f1"], b["high"]["f1"]),
        (f"MED   precision",  a["medium"]["precision"], b["medium"]["precision"]),
        (f"MED   recall",     a["medium"]["recall"], b["medium"]["recall"]),
        (f"MED   F1",         a["medium"]["f1"], b["medium"]["f1"]),
    ]
    for label, va, vb in rows:
        print(f"{label:24}{va:>32.3f}{vb:>32.3f}   {delta(va, vb)}")
    print("-" * 100)
    print(f"{'thresholds (med/high)':24}"
          f"{str(a['medium_t']) + '/' + str(a['high_t']):>32}"
          f"{str(b['medium_t']) + '/' + str(b['high_t']):>32}")
    better = "B" if b["auprc"] > a["auprc"] else "A" if a["auprc"] > b["auprc"] else "tie"
    print(f"\nHigher AUPRC (the imbalance-aware summary): {better}. Tune offline, then activate the winner.")


def score_histogram(claims):
    buckets = list(range(0, 110, 10))  # 0-9,10-19,...,100
    hist = []
    for lo in buckets:
        hi = lo + 9
        fraud = sum(1 for c in claims if lo <= c["score"] <= hi and c["is_fraud"])
        legit = sum(1 for c in claims if lo <= c["score"] <= hi and not c["is_fraud"])
        hist.append({"bucket": f"{lo}", "lo": lo, "fraud": fraud, "legit": legit})
    return hist


def main():
    ap = argparse.ArgumentParser(description="Evaluate the ClaimLens fraud scorer.")
    ap.add_argument("--live", nargs=3, metavar=("BASE_URL", "EMAIL", "PASSWORD"),
                    help="Pull REAL labelled outcomes from a running app instead of synthetic data, "
                         "e.g. --live http://localhost:8080/api/v1 admin@demo.claimlens.app 'Password123!'")
    ap.add_argument("--compare", nargs=2, metavar=("CONFIG_A", "CONFIG_B"),
                    help="A/B two ruleset configs (weights + thresholds) on the SAME labelled set, "
                         "e.g. --compare config-default.json config-tuned.json")
    args = ap.parse_args()

    if args.live:
        source = "live"
        claims = fetch_live(*args.live)
        if not claims:
            print("No labelled claims yet — decide some claims (approve / reject-as-fraud) first.")
            return
    else:
        source = "synthetic"
        claims = generate_dataset()

    # A/B comparison mode: re-score the labelled claims under two configs and diff — then stop.
    if args.compare:
        with open(args.compare[0]) as f:
            cfg_a = json.load(f)
        with open(args.compare[1]) as f:
            cfg_b = json.load(f)
        compare_configs(claims, cfg_a, cfg_b)
        return

    n = len(claims)
    n_fraud = sum(c["is_fraud"] for c in claims)
    if n_fraud == 0 or n_fraud == n:
        print(f"Source={source}: {n} claims, {n_fraud} fraud — need both classes to evaluate. "
              "Decide more claims of each kind.")
        return

    report = {
        "meta": {
            "source": source,
            "n_claims": n, "n_fraud": n_fraud, "fraud_rate": round(n_fraud / n, 4),
            "seed": SEED,
            "rules": {c: {"weight": w, "label": l} for c, (w, l) in RULES.items()},
            "medium_threshold": MEDIUM_THRESHOLD, "high_threshold": HIGH_THRESHOLD,
            "p_fire": {c: {"legit": P_FIRE[c][0], "fraud": P_FIRE[c][1]} for c in RULES},
        },
        "operating_points": {
            "HIGH (score>=50)": rates(*confusion(claims, HIGH_THRESHOLD)),
            "MEDIUM (score>=25)": rates(*confusion(claims, MEDIUM_THRESHOLD)),
        },
        "roc_auc": round(roc_auc_mann_whitney(claims), 4),
        "roc_curve": roc_curve(claims),
        "per_rule": per_rule_lift(claims),
        "histogram": score_histogram(claims),
    }
    pr_pts, auprc = pr_curve_and_auprc(claims)
    report["pr_curve"] = pr_pts
    report["auprc"] = round(auprc, 4)

    out_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "report.json")
    with open(out_path, "w") as f:
        json.dump(report, f, indent=2)

    # Console summary
    hp = report["operating_points"]["HIGH (score>=50)"]
    mp = report["operating_points"]["MEDIUM (score>=25)"]
    print(f"Source: {source.upper()}  -  {n} claims, {n_fraud} fraud ({report['meta']['fraud_rate']:.0%} base rate)")
    print(f"ROC AUC = {report['roc_auc']:.3f}   AUPRC = {report['auprc']:.3f}  (baseline={n_fraud/n:.3f})")
    print(f"\nHIGH (>=50):   precision={hp['precision']:.2f}  recall={hp['recall']:.2f}  "
          f"F1={hp['f1']:.2f}  [TP{hp['tp']} FP{hp['fp']} FN{hp['fn']} TN{hp['tn']}]")
    print(f"MEDIUM (>=25): precision={mp['precision']:.2f}  recall={mp['recall']:.2f}  "
          f"F1={mp['f1']:.2f}  [TP{mp['tp']} FP{mp['fp']} FN{mp['fn']} TN{mp['tn']}]")
    print("\nPer-rule lift (of firings, % fraud | of frauds, % caught):")
    for r in report["per_rule"]:
        print(f"  {r['code']:<26} w={r['weight']:>2}  fired={r['fired']:>3}  "
              f"precision={r['precision']:.2f}  recall={r['recall']:.2f}")
    print(f"\nWrote {out_path}")


if __name__ == "__main__":
    main()
