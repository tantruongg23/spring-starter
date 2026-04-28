---
stepsCompleted: []
inputDocuments:
  - "{project-root}/docs/JWT_SECURITY.md"
workflowType: 'research'
lastStep: 1
research_type: 'technical'
research_topic: 'Robust JWT security hardening + migration plan for Spring Security OAuth2 Resource Server (RS/ES keys, strict iss/aud, cookie+CSRF browser flow, opaque hashed refresh tokens, rotation, and rate limiting)'
research_goals: 'Produce an implementation-ready, source-cited plan to migrate the current HS256 + plaintext refresh-JWT-in-DB approach to a more robust design: A2 (browser cookie access + non-browser bearer), C1 (CSRF enabled for cookie flow), R1+R2 (browser refresh cookie + non-browser refresh in body), O1 strict CORS, S2/K1 asymmetric keys with JWKS + rotation, I1 strict issuer+audience validation, T1 jti+sid, F1/D1/X1 opaque hashed refresh with atomic rotation and DoS-resistant replay response (U-A), RL-A rate limits, E-A per-env separation, access=10m refresh=14d. Include concrete Spring configuration patterns and migration steps for this repository.'
user_name: 'Tony'
date: '2026-04-28'
web_research_enabled: true
source_verification: true
---

# Research Report: technical

**Date:** 2026-04-28  
**Author:** Tony  
**Research Type:** technical

---

## Research Overview

This report will research and propose a robust JWT architecture and a migration plan for this repository’s Spring Security setup, with current-source verification and clear implementation steps.

---

<!-- Content will be appended sequentially through research workflow steps -->

