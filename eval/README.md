# Framing judge evaluation

## What this tests

Can an LLM apply a framing checklist the same way a person does?

A person and an LLM judge answer the same three yes/no questions from
[`docs/framing-rubric.md`](../docs/framing-rubric.md) for the same 30 short texts:

1. **Charged words**: does the text use words chosen to provoke emotion rather than inform?
2. **Built-in blame**: does it say who is at fault without saying how that is known?
3. **Only one side**: does it present only one viewpoint on a disputed issue?

The answers are then compared to see how often the two agree, and the disagreements are reviewed one by one.

## How the data was made

[`data/texts.csv`](data/texts.csv) holds 30 short invented news-style texts about fictional places and events,
in the style of the vignettes used in psychology studies. Using invented texts means every text can be published
here in full, so anyone can read exactly what was labeled.

- The texts were drafted with an LLM and reviewed and edited by hand.
- They were designed so each question is answered "yes" for roughly 40% of texts, in different combinations.
- A few are deliberate borderline cases that test the checklist's rules (e.g. a single mildly charged word, or
  a token mention of the other side).
- The texts are shuffled so similar cases don't sit next to each other.

**Limitation:** invented texts are cleaner and shorter than real news, and LLM-drafted text may be easier for an
LLM to judge. Results here are a first check of the checklist and the judge, not a measure of real-world accuracy.

## Process

1. **Human labels**: each text is labeled by hand against the checklist, *before* the LLM judge is run, so the
   labels aren't influenced by its answers. Stored in [`data/human_labels.csv`](data/human_labels.csv)
   (`yes`/`no` per question).
2. **LLM judge**: the checklist is included in the judge's prompt, and it returns a yes/no answer per question for
   each text.
3. **Agreement**: percent agreement and Cohen's kappa per question.
4. **Error analysis**: every disagreement is classified as a checklist ambiguity, a judge error, or a labeling
   error, and the checklist is revised.

## Files

| File | Contents |
|---|---|
| `data/texts.csv` | `id`, `text` |
| `data/human_labels.csv` | `id`, then `yes`/`no` for each of the three questions |
| `data/labeling_notes.md` | Texts where the checklist felt unclear while labeling |
| `results/judge_labels.csv` | Same columns as `human_labels.csv`, then `model`, `run_at` and the judge's reason for each answer. Texts where the judge call failed are left out (they are listed in the run output). |

## How to run

```bash
export ANTHROPIC_API_KEY=...
./gradlew runEval
```

This sends each text to the judge and writes `results/judge_labels.csv`, replacing the previous run. Set
`JUDGE_MODEL` to use a different model (default `claude-sonnet-5-5`).
