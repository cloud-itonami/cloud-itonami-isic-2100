(ns pharma.advisor
  "Pharmaceutical Operations Advisor -- the LLM-driven suggestion layer.
  Proposes operations to the Governor for approval.

  NOTE: This advisor suggests COORDINATION and PAPERWORK operations only.
  It does NOT control formulation, active ingredients, QP sign-off, or batch release.")

;; ----------------------------- mock advisor for testing -----------------------------

(defn mock-advisor
  "Create a mock advisor for testing. Real implementation would call an LLM."
  []
  {:type :mock :model "mock-v1"})

(defn log-batch-record-proposal
  "Propose a batch manufacturing record checkpoint (receipt of materials, blending complete, etc.)."
  [_advisor batch-id checkpoint-type]
  {:op :actuation/log-batch-record
   :subject batch-id
   :effect :propose
   :cites ["Pharmaceutical Affairs Law (医薬品医療機器等法) §27"
           "MHLW Good Manufacturing Practice Guidelines (厚生労働省GMP基準)"]
   :value {:evidence {:batch-verified true :checkpoint-complete true :lab-notes true}
           :confidence 0.88
           :detail (str "Batch " batch-id " checkpoint " checkpoint-type " recorded")}})

(defn schedule-maintenance-proposal
  "Propose equipment maintenance or calibration scheduling."
  [_advisor equipment-id maintenance-type]
  {:op :actuation/schedule-maintenance
   :subject equipment-id
   :effect :propose
   :cites ["Pharmaceutical Affairs Law §27"
           "MHLW Good Manufacturing Practice Guidelines"]
   :value {:evidence {:maintenance-schedule-current true :safety-review-done true}
           :confidence 0.85
           :detail (str "Equipment " equipment-id " " maintenance-type " scheduled")}})

(defn flag-deviation-proposal
  "Propose flagging a GMP deviation or quality issue.
  ALWAYS escalates to human -- never silently logged.
  is-deviation?: boolean indicating if this is a true deviation vs. noise."
  [_advisor batch-id deviation-type is-deviation?]
  {:op :actuation/flag-deviation
   :subject batch-id
   :effect :propose
   :cites ["Pharmaceutical Affairs Law §27"
           "MHLW Good Manufacturing Practice Guidelines"]
   :value {:evidence {:deviation-documented true :impact-assessed true}
           :confidence 0.82
           :is-deviation? is-deviation?
           :detail (str "Batch " batch-id " deviation flagged: " deviation-type)}})

(defn request-batch-release-review-proposal
  "Propose scheduling a qualified person (QP) batch-release review.
  The actor NEVER releases the batch itself -- only requests the QP review.
  This ALWAYS escalates to human."
  [_advisor batch-id qc-results-id]
  {:op :actuation/request-batch-release-review
   :subject batch-id
   :effect :propose
   :cites ["Pharmaceutical Affairs Law §14"
           "MHLW Good Manufacturing Practice Guidelines §23 (QP duties)"]
   :value {:evidence {:qc-testing-complete true :coa-on-file true :batch-history-clean true}
           :confidence 0.90
           :qc-results-id qc-results-id
           :detail (str "Batch " batch-id " ready for qualified person release review")}})
