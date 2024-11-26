(ns pez.baldr-test
  (:require [pez.baldr :as sut]
            [clojure.test :refer [deftest is testing]]))

(def ^:private config {:color #(str "|t " % " t|")
                       :bullet "*"
                       :bullet-color #(str "|b " % " b|")
                       :context-color #(str "|c " % " c|")})

(def ^:private sut-get-report @#'sut/get-report)

(deftest get-report
  (testing "No previously seen contexts"
    (testing "Without context"
      (is (= ["    |b * b| |t message t|"]
             (sut-get-report {:message "message"} [] [] config))
          "prints the message at the top indent level"))

    (testing "With context"
      (is (= ["    |c context 1 c|"
              "      |b * b| |t message t|"]
             (sut-get-report {:message "message"} ["context 1"] [] config))
          "a single context prints at the top indent level, with the message indented at the next indent level")
      (is (= ["    |c context 1 c|"
              "      |c context 2 c|"
              "        |b * b| |t message t|"]
             (sut-get-report {:message "message"} ["context 2" "context 1"] [] config))
          "contexts nest each other, with the message indented nested deepest")
      (is (= ["    |c context 1 c|"
              "      |c context 1 c|"
              "        |b * b| |t message t|"]
             (sut-get-report {:message "message"}
                             ["context 1" "context 1"]
                             []
                             config))
          "identical context texts at different levels are treated as distinct nesting levels")))

  (testing "With a previously seen context"
    (testing "Without context"
      (is (= ["    |b * b| |t message t|"]
             (sut-get-report {:message "message"} [] ["context 1"] config))
          "a seen context is ignored")
      (is (= ["    |b * b| |t message t|"]
             (sut-get-report {:message "message"} [] ["context 1" "context 2"] config))
          "more than on seen contexts are also ignored"))

    (testing "With context"
      (testing "Matching seen context"
        (is (= ["      |b * b| |t message t|"]
               (sut-get-report {:message "message"} ["seen context 1"] ["seen context 1"] config))
            "a matching seen context is not printed, but makes the message indent one level deeper"))

      (testing "Non-matching seen context"
        (is (= ["    |c unseen context 1 c|"
                "      |b * b| |t message t|"]
               (sut-get-report {:message "message"} ["unseen context 1"] ["seen context 1"] config))
            "prints at the top level, with the message indented at the next level")
        (is (= ["    |c unseen context 1 c|"
                "      |c unseen context 2 c|"
                "        |b * b| |t message t|"]
               (sut-get-report {:message "message"} ["unseen context 2" "unseen context 1"] ["seen context 1"] config))
            "non-matched seen contexts are unseen, regardless how many")
        (is (= ["    |c unseen context 1 c|"
                "      |c unseen context 2 c|"
                "        |b * b| |t message t|"]
               (sut-get-report {:message "message"}
                               ["unseen context 2" "unseen context 1"]
                               ["unseen context 1" "unseen context 2"]
                               config))
            "the order of the seen contexts matters"))

      (testing "Matching higher level seen context, non-matching lower level seen context"
        (is (= ["      |c new context 2 c|"
                "        |b * b| |t message t|"]
               (sut-get-report {:message "message"}
                               ["new context 2" "seen context 1"]
                               ["seen context 2" "seen context 1"]
                               config))
            "only prints unseen contexts while indenting the message under all context levels")))))
