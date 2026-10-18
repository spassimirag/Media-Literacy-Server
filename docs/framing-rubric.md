# Framing checklist

For each text, answer yes or no to each question.

## What this measures

This checklist measures **emphasis framing**: how a text selects and highlights some aspects of a story
to push the reader toward a particular interpretation. It follows Entman's (1993) definition, where
framing means making certain aspects of reality more salient in order to promote a particular problem
definition, causal interpretation, moral evaluation, or recommended action.

It does **not** measure equivalence framing, i.e. logically identical facts described differently, such as
"200 people will be saved" vs. "400 people will die" (Tversky & Kahneman, 1981), because that can only be
seen by comparing two versions of a text. It also does not judge whether a text is true.

### References

- Entman, R. M. (1993). Framing: Toward clarification of a fractured paradigm. *Journal of Communication*, 43(4),
  51–58.
- Tversky, A., & Kahneman, D. (1981). The framing of decisions and the psychology of choice. *Science*, 211(4481),
  453–458.

## 1. Charged words

Does the text use words chosen to provoke emotion rather than inform?

**Rule:** answer yes if there are two or more charged words, or one strongly charged word (e.g. "dumping", "regime",
"thugs"). A single common word like "slashed" on its own is not enough.

- Yes example: "A tech company slashed its workforce, dumping hundreds of loyal, hard-working employees onto the street
  just weeks before the holidays."
- No example: "Tech company reduced its staff by 12% in the fourth quarter to lower overall operating costs."

## 2. Built-in blame

Does the text say who is at fault without saying how that is known?

**Rule:** answer no if the blame comes from a named source (police, a court, an investigation, a named witness).
Answer yes if the text states fault as fact on its own authority, even in neutral words.

- Yes example: "The bus driver caused the collision on Route 4 on Friday morning by running a red light."
- No example: "Police said the bus driver ran a red light before the collision on Route 4 on Friday morning."
- No example: "Two vehicles, including a transit bus, collided on Route 4 on Friday morning. Police are currently
  investigating the cause of the accident."

## 3. Only one side

Does the text present only one viewpoint on a disputed issue?

**Rule:** the other side only counts if its view is actually stated, not just mentioned (e.g. "though some critics
complained" does not count).

A **disputed issue** is a policy, decision or proposal on which people take opposing positions. Words like
"unpopular", "controversial" or "victory" show that an issue is disputed. An event (e.g. a storm or a car crash) is
not a disputed issue, even if the text blames someone for it: answer no. Blaming someone without giving their side
is covered by question 2, not this one.

- Yes example: "The city passed the new curfew law after supporters argued it would keep neighborhoods safe."
- Yes example: "The city passed the new curfew law after supporters argued it would keep neighborhoods safe, though
  some critics complained."
- No example: "The city passed the new curfew law, which supporters say improves safety but critics argue limits
  personal freedom."
- No example: "A reckless driver caused a crash on Main Street on Thursday." (an event, not a disputed issue; the
  blame is a question 2 matter)

## Changes in v2

Question 3 now defines "disputed issue", says that signals like "unpopular" count, and makes clear that blame for an
event belongs to question 2. See [`error-analysis.md`](error-analysis.md) for why. The v1 human labels in
`eval/data/human_labels.csv` were made with the previous version.

**v2 has not been evaluated yet:** the texts were not labeled again and the judge was not run again under v2, so
all published agreement numbers are for v1.
