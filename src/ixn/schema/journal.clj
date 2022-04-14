(ns ixn.schema.journal)

(def JournalType [:enum [:sales :purchase :memo :cash-bank]])

(defonce journal-params {:purchase {:accounts-payable "22000"}
                         :sales    {:accounts-receivable "12010"}})
