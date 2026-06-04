---
apply: always
---

When creating unittests, @DisplayName shall be used to explain what the test is doing, and what is expected.
The @DisplayName is to be formatted in this way:
@DisplayName(
"""
Når <kontekst>
Hvis <betingelse>
Så:
<forventning>
"""
)
