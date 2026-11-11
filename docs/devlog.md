

## 2026-11-11 — State-aware Today learning CTA

- **Scope:** Add the prominent full-width pill action to the Continue Learning card while keeping the subject/topic as its first line and not restoring an all-caps “CONTINUE LEARNING” kicker. The button is now the only card action, matching the supplied visual reference’s hierarchy.
- **State mapping:** The action says “Continue learning” for an unfinished saved lesson; “Start a lesson” when there is no usable saved lesson or the next lesson is still within an unfinished topic; and “Start a topic” when the saved topic is complete and the next available target belongs to another topic. The selected target is persisted and opened through the existing lesson flow.
- **Completion edge case:** If every available lesson is complete, Today shows “All lessons complete” with a “Browse topics” action rather than sending the student back to the first completed lesson. When no lesson content is available, the existing “Choose a subject to begin” state remains.
- **Tests/checks:** Added unit coverage for the four state-label paths and Android UI coverage for the visible clickable button, subject/topic first line, absence of the top kicker, and click callback. Full PR CI is pending. Changes will be verified on the final PR head before merging.
