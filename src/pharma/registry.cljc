(ns pharma.registry
  "Proposal registry and drafting helpers for pharmaceutical operations.
  Every proposal carries its spec-basis and evidence checklist.

  CRITICAL: This actor does NOT release batches. The registry helps the
  operator coordinate paperwork, but batch release remains QP-exclusive.")

;; ----------------------------- hard invariants -----------------------------

(defn hard-invariant-violations
  "Hard invariants that CANNOT be overridden:
  - If operation affects batch status or GMP compliance, it must carry spec-basis.
  - Batch must be registered before any action."
  [op-type subject]
  (when (contains? #{:actuation/log-batch-record
                     :actuation/flag-deviation
                     :actuation/request-batch-release-review} op-type)
    (when (empty? subject)
      [{:rule :no-subject
        :detail "提案に対象バッチIDが無い"}])))

;; ----------------------------- protected operations -----------------------------

(defn protected-operation-violations
  "Operations that require human sign-off and can never be autonomous:
  - Deviation flagging (GMP deviations)
  - Batch-release review requests (QP exclusive authority)

  NOTE: We do NOT list :actuation/log-batch-record as protected,
  because routine batch record logging (materials in, blending done, etc.)
  can be proposed with soft escalation only if evidence is strong."
  [op-type]
  (when (contains? #{:actuation/flag-deviation
                     :actuation/request-batch-release-review} op-type)
    [{:rule :requires-human-approval
      :detail "このオペレーションには人間の承認が必須"}]))

;; ----------------------------- proposal drafts -----------------------------

(defn log-batch-record-draft
  "Draft a batch manufacturing record checkpoint.
  subject: batch ID
  cites: spec-basis citations (GMP regulations, ICH guidelines, etc.)
  checkpoint-type: e.g., :materials-received, :blending-complete, :testing-started
  evidence-checklist: map of verified evidence items"
  [subject cites checkpoint-type evidence-checklist confidence detail]
  {:op :actuation/log-batch-record
   :subject subject
   :effect :propose
   :cites cites
   :value {:evidence evidence-checklist
           :confidence confidence
           :checkpoint checkpoint-type
           :detail detail}})

(defn schedule-maintenance-draft
  "Draft equipment maintenance or calibration scheduling.
  subject: equipment ID
  cites: spec-basis citations
  maintenance-type: e.g., :calibration, :preventive-maintenance, :cleaning-validation
  evidence-checklist: map of verified evidence items"
  [subject cites maintenance-type evidence-checklist confidence detail]
  {:op :actuation/schedule-maintenance
   :subject subject
   :effect :propose
   :cites cites
   :value {:evidence evidence-checklist
           :confidence confidence
           :maintenance-type maintenance-type
           :detail detail}})

(defn flag-deviation-draft
  "Draft a GMP deviation or quality issue flag.
  subject: batch ID
  cites: spec-basis citations
  deviation-type: e.g., :temperature-excursion, :missing-record, :contamination-risk
  is-deviation?: true if this is a real deviation, false if ruled out after investigation
  evidence-checklist: map of verified evidence items"
  [subject cites deviation-type is-deviation? evidence-checklist confidence detail]
  {:op :actuation/flag-deviation
   :subject subject
   :effect :propose
   :cites cites
   :value {:evidence evidence-checklist
           :confidence confidence
           :deviation-type deviation-type
           :is-deviation? is-deviation?
           :detail detail}})

(defn request-batch-release-review-draft
  "Draft a qualified person (QP) batch-release review request.
  subject: batch ID
  cites: spec-basis citations (QP duties, etc.)
  qc-results-id: ID of the QC testing record
  evidence-checklist: map of verified evidence items
  NOTE: This actor requests the review. The QP performs the actual release/rejection."
  [subject cites qc-results-id evidence-checklist confidence detail]
  {:op :actuation/request-batch-release-review
   :subject subject
   :effect :propose
   :cites cites
   :value {:evidence evidence-checklist
           :confidence confidence
           :qc-results-id qc-results-id
           :detail detail}})
