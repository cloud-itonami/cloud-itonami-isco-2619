(ns legalcompliance.store
  "SSoT for the ISCO-08 2619 independent legal support & compliance
  practice actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md
  Actors section; README's 'Robotics premise' — a document
  notarization and certification-support robot performs physical
  stamping, seal application and certified-copy binding under this
  advisor/governor pair, which never dispatches hardware itself and
  never issues a notarization or certification). Modeled on
  cloud-itonami-isco-4311's bookkeeping.store.

  Domain:

    client   — a registered individual/organization (:client-id, :name)
    document — a registered document submission {:document-id
               :client-id :name :max-certified-copies number
               :identity-verified? boolean}. `:max-certified-copies`
               is the registered authorized quantity a proposed
               certification's certified-copy count must not exceed —
               issuing more certified copies than authorized is
               unregulated duplication, not efficient service.
               `:identity-verified?` records whether the submitter's
               identity has been verified — certifying a document
               without verified identity is a notarization fraud risk,
               not efficient service.
    record   — a committed operating record (an issued certification)
               — written ONLY via commit-record!.
    ledger   — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (document [s document-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-document! [s d])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (document [_ document-id] (get-in @a [:documents document-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-document! [s d]
    (swap! a assoc-in [:documents (:document-id d)] d) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :documents {} :records [] :ledger []}
                                   seed)))))
