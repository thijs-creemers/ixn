(ns crux-in-a-box.schema.account-test
  (:require [clojure.test :refer [deftest is testing]]
            [crux-in-a-box.schema.account :refer [create-account calc-summary-level]]))

(deftest test-create-account
         (testing "happy flow create an account"
                  (let [{:keys [status value]} (create-account {:account/id   "12345"
                                                                :account/name "my asset"
                                                                :account/type :ast})]
                       (is (= status true))
                       (is (= {:account/id            "12345"
                               :account/name          "my asset"
                               :account/type          :ast
                               :account/summary-level 0} value))))

         (testing "wrong account number"
                  (let [{:keys [status]} (create-account {:account/id   "A2"
                                                          :account/name "my asset"
                                                          :account/type :ast})]
                       (is (= status false))))

         (testing "empty description"
                  (let [{:keys [status]} (create-account {:account/id   "12345"
                                                          :account/name ""
                                                          :account/type :ast})]
                       (is (= status false))))

         (testing "account type"
           (let [{:keys [status]} (create-account {:account/id   "12345"
                                                   :account/name "Wrong account type"
                                                   :account/type :asv})]
             (is (= status false))))

         (testing "calc-summary-level"
                  (is (= 0 (calc-summary-level "12345")))
                  (is (= 1 (calc-summary-level "1234")))
                  (is (= 2 (calc-summary-level "123")))
                  (is (= 3 (calc-summary-level "12")))
                  (is (= 4 (calc-summary-level "1")))
                  (is (= -1 (calc-summary-level "1234515151"))))

         (testing "summary-level in account"
                  (let [{:keys [status value]} (create-account {:account/id   "1"
                                                                :account/name "sum level 4"
                                                                :account/type :ast})]
                       (print value)
                       (is (= status true))
                       (is (= (:account/summary-level value) 4)))

                  (let [{:keys [status value]} (create-account {:account/id   "123"
                                                                :account/name "sum level 2"
                                                                :account/type :ast})]
                       (is (= status true))
                       (is (= (:account/summary-level value) 2)))))


;(deftest test-update-account [])

;(deftest test-remove-account [])

;(deftest import-account-fixtures [])