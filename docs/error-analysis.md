# Error analysis: judge vs. human labels

The judge's answers (`eval/results/judge_labels.csv`, run of 2026-10-17 with `claude-sonnet-5-5`) were compared
with the human labels (`eval/data/human_labels.csv`) for the 30 eval texts. This document reviews every disagreement.

## Agreement

| Question | Agreed |
|---|---|
| Charged words | 30/30 (100%) |
| Built-in blame | 30/30 (100%) |
| Only one side | 26/30 (87%) |

All four disagreements are on **only one side**. Two go each way (judge yes / human no, and the reverse), so the
judge is not simply stricter or more lenient than the human labeler.

## The disagreements

Each one is classified as:

- **Unclear rubric**: the rule allows both readings
- **Judge error**: the judge misapplied a clear rule
- **Labeling error**: on reflection, the human label was wrong

The human's reasons were written down before seeing a second opinion.

### 017: labeling error

> Parents celebrated a huge victory on Monday as the district scrapped its unpopular homework policy, a move they
> say will give exhausted children their evenings back.

| Human | Judge |
|---|---|
| no | yes |

The human labeler didn't read the homework policy as a disputed issue. But "unpopular" and "huge victory" signal that
there is another side, and only the parents' view is stated, so the judge is right. The rubric never defines
"disputed issue", which is why this text was flagged as unclear in `eval/data/labeling_notes.md` during labeling.

### 021: unclear rubric

> The flooding in the lower town was likely made worse by the city's decision to delay drainage upgrades.

| Human | Judge |
|---|---|
| yes | no |

The human said yes because the text blames the city without giving the city's point of view. That is what
question 2 (built-in blame) measures, and both raters already said yes there. For question 3, the text is about a
flood, which the rubric says to answer no for, but it also blames a policy decision. The rule doesn't say what to do
when a text about an event blames a decision.

### 028: labeling error, with a rubric gap

> The decline in test scores at Harlow High followed the school's decision to cut tutoring programs, which caused
> students to fall behind. Teachers' representatives said the cuts should be reversed.

| Human | Judge |
|---|---|
| no | yes |

The tutoring cuts are a disputed decision, and only the teachers' representatives' view is stated, so by the rule
as written the judge is right. The human's reasoning was that the school may not have wanted to comment, leaving
the teachers' view as the only one available. The rubric doesn't say how to handle a side that declined to comment.

### 030: labeling error

> A drunk, reckless driver smashed into a packed bus stop on Main Street on Thursday, leaving three people horribly
> injured.

| Human | Judge |
|---|---|
| yes | no |

A crash is explicitly listed in the rubric as not a disputed issue, so the answer is no. The human said yes because
the driver might have another point of view, which, as with 021, is reasoning about blame rather than viewpoints.

## Findings

1. **No clear judge errors.** The judge applied the rubric literally in all four cases. The disagreements come from
   the human labeler and from gaps in the rubric.
2. **"Only one side" and "built-in blame" overlap in practice.** In both texts where the human said yes and the
   judge said no (021, 030), the human's reason was that someone was blamed without their side being given. That
   belongs to question 2. The rubric doesn't separate the two questions clearly enough, so a human labeler can blur
   them.
3. **"Disputed issue" is undefined.** This caused 017 and contributed to 021.

## Proposed rubric v2 changes (question 3)

1. **Define "disputed issue":** a policy, decision or proposal on which people take opposing positions. An event
   (a storm, a crash) is not a disputed issue, even if the text blames someone for it.
2. **Point blame to question 2:** if the text blames someone without giving their side, that is built-in blame,
   not "only one side".
3. **Signals of disagreement count:** words like "unpopular", "controversial" or "victory" show that an issue is
   disputed.
4. **Open question: a side that declined to comment.** The rubric should say whether "the school declined to
   comment" counts as presenting only one side. Not yet decided.

## Status of v2

Changes 1–3 are now in `docs/framing-rubric.md` (v2). Change 4 is still open.

**v2 has not been evaluated.** It was written from these results, but the texts were not labeled again and the
judge was not run again under v2. All agreement numbers in this document are for v1. Evaluating v2 would mean
labeling texts under v2 into a new file (keeping the v1 labels in `eval/data/human_labels.csv`), running the judge
again and comparing. Ideally this would use new texts, since the 30 here have now been discussed in detail.

## Limitations

- There is only one human labeler, so labeling errors could only be caught by comparing against the judge and
  re-reading. A second human labeler would make this check stronger.
