(ns wundernut.analyzer-test
  (:require [wundernut.analyzer :as sut]
            [clojure.test :refer :all]))

(deftest e-2-e-test-with-input-wav
  (testing "Check test file parsing result matching expected"
    (is (= "MAY WE TOGETHER BECOME GREATER THAN THE SUM OF BOTH OF US. SAREK." (sut/morse->text "message.wav")))))

(deftest test-average-pairs
  (testing "simple"
    (is (= [5 55] (transduce sut/pair-average conj [1 10 100])))))

(deftest test-remove-consecutive
  (testing "simple consecutive removal"
    (is (= [1] (transduce sut/remove-consecutive conj [1 2 3 4 5])))
    (is (= [2 6] (transduce sut/remove-consecutive conj [2 3 6 7 8])))
    (is (= [2 6 100] (transduce sut/remove-consecutive conj [2 3 6 7 100 101])))
    (is (= [2 6 100] (transduce sut/remove-consecutive conj [2 3 6 100 101])))))

(deftest test-consecutive-removed-averaged
  (testing "simple"
    (is (= [13] (transduce sut/non-consecutive-average conj '(7 8 20 21))))
    (is (= [13 33] (transduce sut/non-consecutive-average conj '(7 8 20 21 46 47))))))
