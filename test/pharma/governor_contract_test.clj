(ns pharma.governor-contract-test
  (:require [clojure.test :refer [deftest is]]
            [pharma.store :as store]
            [pharma.advisor :as advisor]
            [pharma.governor :as governor]))

(deftest spec-basis-hard-gate
  "Spec-basis is a HARD gate: never allow proposals without official GMP citations."
  (let [st (store/mem-store)
        proposal {:op :actuation/log-batch-record
                  :subject "batch-2024-001"
                  :effect :propose
                  :value {:evidence {:batch-verified true}
                          :confidence 0.9}
                  :cites []}]
    (let [eval (governor/evaluate proposal st)]
      (is (:holds? eval) "Proposal with empty cites should hold")
      (is (seq (:hard-violations eval)) "Should have hard violations")
      (is (some #(= (:rule %) :no-spec-basis) (:hard-violations eval))))))

(deftest batch-registration-hard-gate
  "Batch must be registered before any manufacturing action.
  This is a HARD block: no off-the-books batches."
  (let [st (store/mem-store)
        proposal {:op :actuation/log-batch-record
                  :subject "batch-unknown"
                  :effect :propose
                  :cites ["GMP Guideline §27"]
                  :value {:evidence {:batch-verified true}
                          :confidence 0.9}}]
    (let [eval (governor/evaluate proposal st)]
      (is (:holds? eval) "Proposal for unregistered batch should hold")
      (is (seq (:hard-violations eval)) "Should have hard violations")
      (is (some #(= (:rule %) :batch-not-registered) (:hard-violations eval))
        "Should block unregistered batch"))))

(deftest qp-release-block
  "HARD BLOCK: Proposals mentioning batch release, QP sign-off, or GMP certification
  are immediately rejected. Those remain QP exclusive authority."
  (let [st (store/mem-store)
        proposal {:op :actuation/log-batch-record
                  :subject "batch-2024-001"
                  :effect :propose
                  :cites ["GMP"]
                  :value {:evidence {:batch-verified true}
                          :confidence 0.9
                          :detail "Please release this batch for distribution to customers"}}]
    (let [eval (governor/evaluate proposal st)]
      (is (:holds? eval) "QP-authority proposal should hold")
      (is (some #(= (:rule %) :qp-exclusive-authority) (:hard-violations eval))
        "Should have qp-exclusive-authority violation"))))

(deftest deviation-escalation
  "Deviations ALWAYS escalate to human. Never silently log a deviation."
  (let [st (store/mem-store)
        proposal {:op :actuation/flag-deviation
                  :subject "batch-2024-001"
                  :effect :propose
                  :cites ["GMP Guideline §27"]
                  :value {:evidence {:deviation-documented true}
                          :confidence 0.95
                          :is-deviation? true
                          :detail "Temperature excursion detected"}}]
    (let [eval (governor/evaluate proposal st)]
      (is (:holds? eval) "Deviation should hold")
      (is (some #(= (:rule %) :deviation-escalation) (:hard-violations eval))
        "Should have deviation-escalation hard violation"))))

(deftest batch-release-review-escalation
  "Batch-release review requests ALWAYS escalate to human (QP exclusive)."
  (let [st (store/mem-store)
        proposal {:op :actuation/request-batch-release-review
                  :subject "batch-2024-001"
                  :effect :propose
                  :cites ["Pharmaceutical Affairs Law §14"]
                  :value {:evidence {:qc-testing-complete true}
                          :confidence 0.92
                          :qc-results-id "qc-001"
                          :detail "Ready for QP review"}}]
    (let [eval (governor/evaluate proposal st)]
      (is (seq (:soft-violations eval)) "Should have soft violations")
      (is (some #(= (:rule %) :batch-release-escalation) (:soft-violations eval))
        "Should escalate batch-release review"))))

(deftest low-confidence-escalates
  "Low confidence proposals escalate to human, even if otherwise clean."
  (let [st (store/mem-store)
        proposal {:op :actuation/schedule-maintenance
                  :subject "equipment-001"
                  :effect :propose
                  :cites ["GMP Guideline"]
                  :value {:evidence {:maintenance-schedule-current true}
                          :confidence 0.45
                          :detail "Schedule equipment calibration"}}]
    (let [eval (governor/evaluate proposal st)]
      (is (seq (:soft-violations eval)) "Should have soft violations")
      (is (some #(= (:rule %) :escalate) (:soft-violations eval))
        "Should escalate low-confidence"))))

(deftest clean-proposal
  "A routine maintenance proposal with all evidence and high confidence is clean."
  (let [st (store/mem-store)
        proposal {:op :actuation/schedule-maintenance
                  :subject "equipment-001"
                  :effect :propose
                  :cites ["GMP Guideline §27"]
                  :value {:evidence {:maintenance-schedule-current true :safety-review-done true}
                          :confidence 0.88
                          :maintenance-type :calibration
                          :detail "Schedule routine calibration of mixing vessel"}}]
    (let [eval (governor/evaluate proposal st)]
      (is (:clean? eval) "Should be clean")
      (is (empty? (:hard-violations eval)) "Should have no hard violations")
      (is (empty? (:soft-violations eval)) "Should have no soft violations"))))

(deftest routine-batch-record-with-high-confidence
  "Routine batch record proposals with high confidence and strong evidence
  are proposed (may still soft-escalate for human approval)."
  (let [st (store/mem-store)
        adv (advisor/mock-advisor)
        proposal (advisor/log-batch-record-proposal adv "batch-2024-001" :blending-complete)]
    (let [eval (governor/evaluate proposal st)]
      ;; No hard violations (batch registered, no QP-exclusive keywords, has spec-basis)
      (is (empty? (:hard-violations eval)) "Should have no hard violations"))))
