(ns orchard.eldoc-test
  (:require [clojure.test :refer :all]
            [orchard.eldoc :as eldoc]
            [orchard.info :as info]))

;; test data
(def test-eldoc-info {:arglists '([x] [x y])})

(def test-eldoc-info-special-form {:forms ['(.instanceMember instance args*)
                                           '(.instanceMember Classname args*)
                                           '(Classname/staticMethod args*)
                                           'Classname/staticField]
                                   :special-form true})

(def test-eldoc-info-candidates
  {:candidates '{X {:arglists ([x])}
                 Y {:arglists ([x] [x y z])}
                 Z {:arglists ([])}}})

(deftest test-extract-arglists
  (is (= (eldoc/extract-arglists test-eldoc-info) '([x] [x y])))
  (is (= (eldoc/extract-arglists test-eldoc-info-candidates)
         '([] [x] [x y z])))
  (is (= (eldoc/extract-arglists test-eldoc-info-special-form)
         '([.instanceMember instance args*]
           [.instanceMember Classname args*]
           [Classname/staticMethod args*]
           [Classname/staticField])))

  ;; sanity checks and special cases
  (is (eldoc/extract-arglists (info/info 'clojure.core 'map)))
  (is (eldoc/extract-arglists (info/info 'clojure.core '.toString)))
  (is (eldoc/extract-arglists (info/info 'clojure.core '.)))
  (is (not (eldoc/extract-arglists (info/info 'clojure.core (gensym "non-existing"))))))

(deftest format-arglists-test
  (is (= (eldoc/format-arglists (eldoc/extract-arglists test-eldoc-info)) '(["x"] ["x" "y"])))
  (is (= (eldoc/format-arglists (eldoc/extract-arglists test-eldoc-info-candidates))
         '([] ["x"] ["x" "y" "z"]))))

;;;; eldoc datomic query
(def testing-datomic-query '[:find ?x
                             :in $ ?name
                             :where
                             [?x :person/name ?name]])

(deftest datomic-query-test
  (testing "eldoc of inline datomic query"
    (let [response (eldoc/datomic-query "user" "'[:find ?x :in $ % ?person-id]")]
      (is (= (:inputs response) '(["$" "%" "?person-id"])))))

  (testing "eldoc of inline datomic query as map"
    (let [response (eldoc/datomic-query "user" "'{:find [?x] :in [$ % ?person-id]}")]
      (is (= (:inputs response) '(["$" "%" "?person-id"])))))

  (testing "eldoc of datomic query defined as symbol"
    (let [response (eldoc/datomic-query "orchard.eldoc-test" "testing-datomic-query")]
      (is (= (:inputs response) '(["$" "?name"])))))

  (testing "eldoc of inline datomic query without :in"
    (let [response (eldoc/datomic-query "user" "'[:find ?x]")]
      (is (= (:inputs response) '(["$"])))))

  (testing "eldoc of inline datomic query as map without :in"
    (let [response (eldoc/datomic-query "user" "'{:find ?x}")]
      (is (= (:inputs response) '(["$"]))))))