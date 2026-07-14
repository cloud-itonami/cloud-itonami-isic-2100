(ns pharma.governor
  "Pharmaceutical Manufacturing Operations Governor -- the independent compliance layer
  that earns the Pharma Operations Advisor the right to propose and log actions.
  The LLM has no notion of GMP (Good Manufacturing Practice) compliance, batch-release
  qualifications, or deviation severity, so this MUST be a separate system able to
  *reject* a proposal and fall back to HOLD.

  HARD violations (a human approver CANNOT override):
    1. Spec-basis       -- no official jurisdiction citation (GMP regs, ICH guidelines, pharmacopoeia)
    2. Plant/Batch registration -- batch must exist in registry before any action
    3. GMP compliance certification -- ALWAYS blocked (batch release is QP exclusive authority)
    4. Batch release authorization -- ALWAYS blocked (qualified person exclusive)

  SOFT violation (can be approved by human):
    5. Confidence floor / escalation gate -- low confidence OR deviation/release-review

  CRITICAL SCOPE BOUNDARY:
  This actor coordinates OPERATIONS AND COMPLIANCE PAPERWORK around pharmaceutical
  manufacturing. It does NOT:
    - Release batches for distribution (QP exclusive authority under GMP regulations worldwide)
    - Certify GMP compliance or product quality (QP exclusive)
    - Control formulation, active ingredient ratios, or process parameters
    - Override deviation protocols (deviations ALWAYS escalate)

  Those remain the exclusive authority of qualified persons (QP) and licensed pharmaceutical engineers."
  (:require [pharma.store :as store]))

(def confidence-floor 0.6)

(def escalation-ops
  "Operations that require human sign-off:
  Deviation flagging, batch-release review requests."
  #{:actuation/flag-deviation :actuation/request-batch-release-review})

(def blocked-keywords
  "Words that indicate production-control or QP-exclusive authority (FORBIDDEN for this actor).
  If a proposal mentions any of these, it's a hard block."
  #{"release-batch" "release" "QP-sign" "QP-certification" "certify-compliance"
    "formulation" "active-ingredient" "dosage-adjustment" "process-parameter"
    "temperature-control" "pressure-control" "pH-adjustment" "solvent-ratio"})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A proposal with no spec-basis citation is a HARD violation --
  never invent a jurisdiction's GMP requirements."
  [proposal _st]
  (let [op (:op proposal)]
    (when (contains? #{:actuation/flag-deviation
                       :actuation/log-batch-record
                       :actuation/request-batch-release-review} op)
      (when (or (empty? (:cites proposal))
                (and (contains? (:value proposal) :spec-basis)
                     (nil? (:spec-basis (:value proposal)))))
        [{:rule :no-spec-basis
          :detail "公式なGMP基準の引用が無い提案は処理できない"}]))))

(defn- plant-batch-registration-violations
  "Batch and plant must be registered before any manufacturing action.
  This is a HARD block: no off-the-books batches."
  [{:keys [op subject]} st]
  (when (contains? #{:actuation/log-batch-record
                     :actuation/flag-deviation
                     :actuation/request-batch-release-review} op)
    (let [batch (store/batch st subject)]
      (when-not batch
        [{:rule :batch-not-registered
          :detail "バッチが登録されていない。製造記録はできない"}]))))

(defn- qp-release-block-violations
  "HARD BLOCK: This actor does NOT release batches or certify GMP compliance.
  If a proposal mentions batch release, QP sign-off, or compliance certification,
  reject it immediately. Those decisions remain the exclusive authority of
  qualified persons (QP) under GMP regulations."
  [proposal _st]
  (let [detail (str (:detail (:value proposal)) " " (:op proposal))
        words (re-seq #"\w+" (.toLowerCase detail))
        forbidden (some #(contains? blocked-keywords %) words)]
    (when forbidden
      [{:rule :qp-exclusive-authority
        :detail (str "バッチ放出・GMP認証は適格者(QP)の排他的権限です。"
                    "この提案には禁止キーワード '" forbidden "' が含まれています。")}])))

(defn- deviation-escalation-violations
  "Deviations ALWAYS escalate to human. Never silently log a deviation."
  [{:keys [op]} {:keys [is-deviation?]}]
  (when (and (= op :actuation/flag-deviation) is-deviation?)
    [{:rule :deviation-escalation
      :detail "GMP逸脱は必ず人間にエスカレートされる"}]))

(defn- batch-release-review-escalation-violations
  "Batch-release review requests ALWAYS escalate to human (QP exclusive)."
  [{:keys [op]} _st]
  (when (= op :actuation/request-batch-release-review)
    [{:rule :batch-release-escalation
      :detail "バッチ放出レビューは適格者の人間の承認が必須"}]))

(defn- confidence-gate-violations
  "Low confidence or escalation-gated operations -> escalate to human."
  [{:keys [op]} {:keys [confidence]}]
  (let [confidence (or confidence 0.5)]
    (when (or (< confidence confidence-floor)
              (contains? escalation-ops op))
      [{:rule :escalate
        :detail (if (< confidence confidence-floor)
                  (str "信頼度が低い (confidence=" confidence ")")
                  "このオペレーションには人間の承認が必要")}])))

;; ----------------------------- governor evaluation -----------------------------

(defn evaluate
  "Evaluate a proposal against all hard and soft gates.
  Returns a map:
    {:holds? boolean
     :hard-violations [...]
     :soft-violations [...]
     :clean? boolean}"
  [proposal st]
  (let [hard-checks-store [spec-basis-violations
                           plant-batch-registration-violations
                           qp-release-block-violations]
        hard-checks-value [deviation-escalation-violations]
        soft-checks [batch-release-review-escalation-violations
                     confidence-gate-violations]
        hard-violations-store (mapcat #(% proposal st) hard-checks-store)
        hard-violations-value (mapcat #(% proposal (:value proposal)) hard-checks-value)
        hard-violations (concat hard-violations-store hard-violations-value)
        soft-violations (mapcat #(% proposal (:value proposal)) soft-checks)]
    {:holds? (seq hard-violations)
     :hard-violations (vec hard-violations)
     :soft-violations (vec soft-violations)
     :clean? (and (empty? hard-violations) (empty? soft-violations))}))
