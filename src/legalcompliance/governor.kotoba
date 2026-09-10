(ns legalcompliance.governor
  "LegalComplianceGovernor — the independent safety/traceability layer
  named in this repository's README/business-model.md, gating every
  certification an advisor may propose for a document. The governor
  never dispatches hardware itself and never issues a notarization or
  certification. Modeled on cloud-itonami-isco-4311's
  bookkeeping.governor. Task twist: a proposed certification's
  certified-copy count is an arithmetic ceiling against the document's
  registered authorized quantity, and a certification cannot be
  committed until the submitter's identity has been verified.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance      — the individual/organization must be
                                registered.
    2. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never issues a notarization or
                                certification; it only gates what the
                                advisor may propose).
    3. document basis         — a certification proposal must cite a
                                REGISTERED document belonging to this
                                client.
    4. certified-copies ceiling — the proposed certified-copy count
                                must not exceed the document's
                                registered `:max-certified-copies`
                                (issuing more certified copies than
                                authorized is unregulated duplication,
                                not efficient service).
    5. identity-verified       — the document must have
                                `:identity-verified?` true before any
                                certification can be committed
                                (certifying a document without
                                verified identity is a notarization
                                fraud risk, not efficient service).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :approve-certification-issuance (no notarization or
                                certification issuance without the
                                governor gate).
    7. :op :approve-regulatory-filing (submitting a regulatory filing
                                on the client's behalf always requires
                                human sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [legalcompliance.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:approve-certification-issuance
                                     :approve-regulatory-filing})

(defn- hard-violations [{:keys [request proposal]} client-record d]
  (let [{:keys [op certified-copies]} proposal
        cert? (= :approve-document-certification op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor は公証・認証を直接発行しない）"})

      (and cert? (nil? d))
      (conj {:rule :unknown-document :detail "未登録 document への認証提案は不可"})

      (and cert? d (not= (:client-id d) (:client-id request)))
      (conj {:rule :document-wrong-client :detail "document が別 client のもの"})

      (and cert? d (number? certified-copies) (> certified-copies (:max-certified-copies d)))
      (conj {:rule :certified-copies-exceeds-authorized
             :detail (str "認証謄本部数 " certified-copies " > 登録済み認可上限 "
                          (:max-certified-copies d) "（認可を超える謄本発行は無許可複製であって効率的サービスではない）")})

      (and cert? d (not (:identity-verified? d)))
      (conj {:rule :identity-not-verified
             :detail "本人確認未完了の document への認証は公証詐欺リスクであって効率的サービスではない"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `legalcompliance.store/Store`. Pure — never
  mutates the store, never issues a notarization or certification."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        d (some->> (:document-id proposal) (store/document store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record d)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
