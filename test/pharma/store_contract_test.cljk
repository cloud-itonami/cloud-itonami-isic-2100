(ns pharma.store-contract-test
  (:require [clojure.test :refer [deftest is]]
            [pharma.store :as store]))

(deftest plant-lookup
  "Plants can be looked up by ID."
  (let [st (store/mem-store)
        plant (store/plant st "plant-001")]
    (is plant "Plant should exist")
    (is (= (:name plant) "Pharmatech Manufacturing Ltd"))
    (is (:license-gmp plant))))

(deftest batch-lookup
  "Batches can be looked up by ID."
  (let [st (store/mem-store)
        batch (store/batch st "batch-2024-001")]
    (is batch "Batch should exist")
    (is (= (:product-name batch) "Ibuprofen 200mg Tablet"))
    (is (:registered? batch))))

(deftest batch-not-found
  "Non-existent batches return nil."
  (let [st (store/mem-store)
        batch (store/batch st "batch-unknown")]
    (is (nil? batch) "Unknown batch should be nil")))

(deftest batch-registered-guard
  "Batch-registered? checks both existence and registration status."
  (let [st (store/mem-store)]
    (is (store/batch-registered? st "batch-2024-001"))
    (is (not (store/batch-registered? st "batch-unknown")))))

(deftest plant-gmp-licensed-guard
  "Plant-gmp-licensed? checks license status."
  (let [st (store/mem-store)]
    (is (store/plant-gmp-licensed? st "plant-001"))
    (is (not (store/plant-gmp-licensed? st "plant-unknown")))))

(deftest starting-material-lookup
  "Starting materials can be looked up by ID."
  (let [st (store/mem-store)
        material (store/starting-material st "sm-001")]
    (is material "Material should exist")
    (is (= (:name material) "Ibuprofen Active Pharmaceutical Ingredient"))
    (is (:coa-verified? material))))

(deftest batch-record-lookup
  "Batch records can be looked up by ID."
  (let [st (store/mem-store)
        record (store/batch-record st "record-2024-001")]
    (is record "Record should exist")
    (is (= (:checkpoint record) :materials-received))))

(deftest deviation-lookup
  "Deviations can be looked up by ID."
  (let [st (store/mem-store)
        dev (store/deviation st "dev-001")]
    (is dev "Deviation should exist")
    (is (= (:type dev) :temperature-excursion))))
