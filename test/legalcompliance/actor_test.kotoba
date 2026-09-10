(ns legalcompliance.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [legalcompliance.actor :as actor]
            [legalcompliance.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Compliance"})
    (store/register-document! st {:document-id "D-1" :client-id "client-1"
                                  :name "document-042"
                                  :max-certified-copies 3
                                  :identity-verified? true})
    st))

(deftest commits-a-within-quantity-verified-certification
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-document-certification :stake :low
                 :document-id "D-1" :certified-copies 2}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-over-quantity-certification
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-document-certification :stake :low
                 :document-id "D-1" :certified-copies 10}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-approves-certification-issuance-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-certification-issuance :stake :low
                 :document-id "D-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
