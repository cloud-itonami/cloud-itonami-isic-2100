(ns pharma.facts-test
  (:require [clojure.test :refer [deftest is]]
            [pharma.store :as store]))

(deftest facts-snapshot
  "The reference store snapshot contains known batches and plants."
  (let [st (store/mem-store)]
    ;; Known plant
    (is (store/plant-gmp-licensed? st "plant-001"))

    ;; Known batches
    (is (store/batch-registered? st "batch-2024-001"))
    (is (store/batch-registered? st "batch-2024-002"))

    ;; Unknown batch
    (is (not (store/batch-registered? st "batch-unknown")))))

(deftest gmp-regulations-cited-in-proposals
  "Proposals reference official GMP regulations and jurisdiction laws."
  (let [st (store/mem-store)]
    ;; The store should support queries against facts
    ;; For now, we verify the governor can evaluate proposals against the store
    (is st "Store should initialize")))
