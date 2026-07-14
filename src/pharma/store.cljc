(ns pharma.store
  "In-memory store for pharmaceutical manufacturing operations state.
  This is a reference implementation; production systems would use Datomic
  or similar persistent event store for audit and replay.")

;; ----------------------------- store initialization -----------------------------

(defn mem-store
  "Create an in-memory store with reference data.
  Pharmaceutical manufacturing batches are tracked with their production status,
  starting materials verification, and intermediate record status."
  []
  {:data (atom {
           :plants {
             "plant-001" {:name "Pharmatech Manufacturing Ltd"
                         :location "Tokyo"
                         :license-gmp true
                         :jurisdiction :JPN}}
           :batches {
             "batch-2024-001" {:plant "plant-001"
                              :product-name "Ibuprofen 200mg Tablet"
                              :status :in-production
                              :batch-size 100000
                              :start-date "2026-07-01T08:00:00Z"
                              :registered? true}
             "batch-2024-002" {:plant "plant-001"
                              :product-name "Acetaminophen 500mg Capsule"
                              :status :in-production
                              :batch-size 50000
                              :start-date "2026-07-10T09:00:00Z"
                              :registered? true}}
           :starting-materials {
             "sm-001" {:name "Ibuprofen Active Pharmaceutical Ingredient"
                      :supplier "Supplier-A"
                      :coa-verified? true
                      :batch-id "batch-2024-001"}
             "sm-002" {:name "Microcrystalline Cellulose"
                      :supplier "Supplier-B"
                      :coa-verified? true
                      :batch-id "batch-2024-001"}}
           :batch-records {
             "record-2024-001" {:batch-id "batch-2024-001"
                               :date "2026-07-05"
                               :checkpoint :materials-received
                               :recorded? true}
             "record-2024-002" {:batch-id "batch-2024-001"
                               :date "2026-07-06"
                               :checkpoint :blending-complete
                               :recorded? true}}
           :deviations {
             "dev-001" {:batch-id "batch-2024-001"
                       :date "2026-07-06T14:30:00Z"
                       :type :temperature-excursion
                       :description "Mixing vessel temperature 23.5C vs spec 22-24C (borderline, corrected)"
                       :severity :minor
                       :escalated? false}}})})

;; ----------------------------- accessors -----------------------------

(defn plant
  "Get plant record by ID."
  [st plant-id]
  (get-in @(:data st) [:plants plant-id]))

(defn batch
  "Get batch record by ID."
  [st batch-id]
  (get-in @(:data st) [:batches batch-id]))

(defn starting-material
  "Get starting material record by ID."
  [st material-id]
  (get-in @(:data st) [:starting-materials material-id]))

(defn batch-record
  "Get batch record checkpoint by ID."
  [st record-id]
  (get-in @(:data st) [:batch-records record-id]))

(defn deviation
  "Get deviation record by ID."
  [st deviation-id]
  (get-in @(:data st) [:deviations deviation-id]))

;; ----------------------------- guards -----------------------------

(defn batch-registered?
  "Check if batch exists and is registered."
  [st batch-id]
  (let [b (batch st batch-id)]
    (and b (:registered? b false))))

(defn plant-gmp-licensed?
  "Check if plant has GMP license for the jurisdiction."
  [st plant-id]
  (let [p (plant st plant-id)]
    (:license-gmp p false)))

(defn materials-verified?
  "Check if all starting materials for a batch have COA verification."
  [st batch-id]
  (let [materials (vals @(-> st :data))
        batch-materials (filter #(= batch-id (:batch-id %)) materials)]
    (every? :coa-verified? batch-materials)))
