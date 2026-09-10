(ns legalcompliance.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [legalcompliance.store :as store]
            [legalcompliance.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Compliance"})
    (store/register-document! st {:document-id "D-1" :client-id "client-1"
                                  :name "document-042"
                                  :max-certified-copies 3
                                  :identity-verified? true})
    st))

(defn- cert-op [copies]
  {:op :approve-document-certification :effect :propose :document-id "D-1"
   :certified-copies copies :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-quantity-and-verified
  (let [st (fresh-store)
        v (governor/check req {} (cert-op 2) st)]
    (is (:ok? v))))

(deftest ok-at-exact-quantity-boundary
  (testing "the certified-copies ceiling is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (cert-op 3) st)]
      (is (:ok? v)))))

(deftest hard-on-certified-copies-exceeds-authorized
  (testing "issuing more certified copies than authorized is unregulated duplication, not efficient service"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (cert-op 10) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :certified-copies-exceeds-authorized (:rule %)) (:violations v))))))

(deftest hard-on-identity-not-verified
  (testing "certifying a document without verified identity is a notarization fraud risk, not efficient service"
    (let [st (store/mem-store)]
      (store/register-client! st {:client-id "client-1" :name "Kobo Compliance"})
      (store/register-document! st {:document-id "D-1" :client-id "client-1"
                                    :name "document-042"
                                    :max-certified-copies 3
                                    :identity-verified? false})
      (let [v (governor/check req {} (assoc (cert-op 2) :confidence 0.99) st)]
        (is (:hard? v))
        (is (some #(= :identity-not-verified (:rule %)) (:violations v)))))))

(deftest hard-on-unknown-document
  (let [st (fresh-store)
        v (governor/check req {} (assoc (cert-op 2) :document-id "D-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-document (:rule %)) (:violations v)))))

(deftest hard-on-foreign-document
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (cert-op 2) st)]
      (is (:hard? v))
      (is (some #(= :document-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (cert-op 2) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (cert-op 2) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-certification-issuance-even-at-high-confidence
  (testing "no notarization or certification issuance without the governor gate"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-certification-issuance :effect :propose
                                    :document-id "D-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-regulatory-filing-even-at-high-confidence
  (testing "submitting a regulatory filing always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-regulatory-filing :effect :propose
                                    :document-id "D-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (cert-op 2) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
