(ns pharma.sim
  "Pharmaceutical operations simulation and demo.
  Demonstrates the governor-advisor contract in action."
  (:require [pharma.store :as store]
            [pharma.advisor :as advisor]
            [pharma.governor :as governor]))

(defn demo
  "Run a demo scenario: propose several operations and evaluate them."
  []
  (let [st (store/mem-store)
        adv (advisor/mock-advisor)

        ;; Scenario 1: Routine batch record logging (should be mostly clean)
        batch-rec-proposal (advisor/log-batch-record-proposal adv "batch-2024-001" :blending-complete)
        batch-rec-eval (governor/evaluate batch-rec-proposal st)

        ;; Scenario 2: Deviation flag (high confidence, minor issue - should soft-escalate)
        dev-proposal (advisor/flag-deviation-proposal adv "batch-2024-001" :temperature-excursion true)
        dev-eval (governor/evaluate dev-proposal st)

        ;; Scenario 3: Batch-release review request (should escalate)
        release-proposal (advisor/request-batch-release-review-proposal adv "batch-2024-001" "qc-results-001")
        release-eval (governor/evaluate release-proposal st)

        ;; Scenario 4: BLOCKED -- proposal mentioning batch release (QP authority violation)
        blocked-proposal {:op :actuation/log-batch-record
                         :subject "batch-2024-001"
                         :effect :propose
                         :cites ["GMP Guideline"]
                         :value {:evidence {:batch-verified true}
                                 :confidence 0.95
                                 :detail "Please release this batch for distribution"}}
        blocked-eval (governor/evaluate blocked-proposal st)]

    {:scenario-1 {:proposal batch-rec-proposal
                  :evaluation batch-rec-eval}
     :scenario-2 {:proposal dev-proposal
                  :evaluation dev-eval}
     :scenario-3 {:proposal release-proposal
                  :evaluation release-eval}
     :scenario-4 {:proposal blocked-proposal
                  :evaluation blocked-eval}}))

(defn -main
  [& _args]
  (let [results (demo)]
    (println "=== Pharmaceutical Manufacturing Operations Demo ===\n")
    (doseq [[scenario {:keys [proposal evaluation]}] results]
      (println (str "Scenario: " scenario))
      (println (str "  Operation: " (:op proposal)))
      (println (str "  Holds? " (:holds? evaluation)))
      (println (str "  Hard violations: " (count (:hard-violations evaluation))))
      (println (str "  Soft violations: " (count (:soft-violations evaluation))))
      (println (str "  Clean? " (:clean? evaluation)))
      (println ""))))
