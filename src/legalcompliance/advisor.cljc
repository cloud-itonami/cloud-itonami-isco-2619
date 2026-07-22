(ns legalcompliance.advisor
  "Compliance Advisor — the advisor named in this repository's README,
  proposing a legal-compliance operation (certify a document, approve
  certification issuance, approve a regulatory filing) from a document
  submission, identity verification and certification request.
  Swappable mock/llm; the advisor ONLY proposes —
  `legalcompliance.governor` checks the certified-copies ceiling and
  identity-verification independently and always escalates
  certification-issuance and regulatory-filing decisions. Modeled on
  cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-document-certification|:approve-certification-issuance|:approve-regulatory-filing
               :effect :propose :document-id str :certified-copies number
               :stake kw :confidence n :rationale str}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake document-id certified-copies] :as request}]
  {:op op
   :effect :propose
   :document-id document-id
   :certified-copies certified-copies
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a legal-compliance advisor. Given a request, propose an
   :op, the :document-id and :certified-copies, an honest :confidence
   and a :stake. Never propose a certified-copy count beyond the
   document's registered authorized quantity, or a certification for
   a document without verified identity — the governor checks both
   against the registered document record. Certification issuance and
   regulatory filing always require human sign-off regardless of
   confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
