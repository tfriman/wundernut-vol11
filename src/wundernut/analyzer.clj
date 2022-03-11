(ns wundernut.analyzer
  (:require [dynne.sampled-sound :refer [read-sound chunks]])
  (:gen-class))

;; Decoded wav files containing Morse code
;; expected file format: RIFF (little-endian) data, WAVE audio, Microsoft PCM, 16 bit, mono 44100 Hz

(def amplitudethreshold
  "Amplitudes higher than these are counter signals being 'on'"
  0.11)

(def hz 44100)

(declare morse-codes)

(defn- ->highs
  "Find indices of those amplitudes going over the threshold and then getting down again"
  [threshold amplitudes]
  (:elems
   (reduce (fn [a [idx v]]
             (let [elems (:elems a)
                   up?   (:up a)]
               (if (and up? (> threshold v))
                 {:up    false
                  :elems (conj elems idx)}
                 (if (and (not up?) (> v threshold))
                   {:up    true
                    :elems (conj elems idx)}
                   a))))
           {:up    false
            :elems []}
           (keep-indexed (fn [idx itm] [idx itm]) amplitudes))))

(defn- ->consecutive-transducer
  "Creates transducer for keeping track of consecutive elements in sequence.
   firstfn is called with 3 normal transducer args  when processing first entry.
   nextfn is called with same args than firstfn plus previous entry is added as last arg."
  [firstfn nextfn]
  (fn [xf]
    (let [last-seen (atom nil)]
      (fn
        ([] (xf))
        ([result] (xf result))
        ([result input]
         (let [previous @last-seen]
           (reset! last-seen input)
           (if previous
             (nextfn xf result input previous)
             (firstfn xf result input))))))))

(def remove-consecutive
  "Transducer for removing consecutive numbers.
  [1 2 3 5 6] becomes [1 5]"
  (->consecutive-transducer
   (fn [xf result input] (xf result input))
   (fn [xf result input previous] (if (= (inc previous) input) result (xf result input)))))

(def pair-average
  "Transducer for averaging (flooring) number pairs in sequence.
  [1 10 100] will result [5 55]"
  (->consecutive-transducer
   (fn [xf result input] result)
   (fn [xf result input previous] (xf result (int (+ previous (/ (- input previous) 2.0)))))))

(def non-consecutive-average
  "Combined transducer for averaging non-consecutive numbers"
  (comp remove-consecutive pair-average))

(defn- ->thresholds
  "Calculate threshold limits for given differences, returns tuple [dashthreshold spacethreshold]"
  [differences]
  (transduce non-consecutive-average conj (sort (keys (frequencies differences)))))


(defn morse->text
  "Parse input file path wav file to text string"
  [input-wav-path]
  (let [input-sound                  (read-sound input-wav-path)
        message-chunks               (chunks input-sound hz)
        amplitudes                   (mapcat first message-chunks) ;; get mono channels to one seq
        highs                        (->> (partition (/ hz 100) amplitudes) ;; downsample to 10 ms partitions
                                          (map #(apply max %))  ;; find max within partition
                                          (->highs amplitudethreshold)) ;; get up&down "timestamps"
        partial-diffs                (map (fn [[a b]] (- b a)) (partition 2 highs)) ;; durations of dits and dashes
        partial-diffs-between        (map (fn [[a b]] (- b a)) (partition 2 (rest highs))) ;; durations between dits and dashes
        [dashthreshold spacethreshold] (->thresholds partial-diffs-between) ;; calculate thresholds
        morse                        (map (fn [i] (if (> dashthreshold i) "." "-")) partial-diffs)
        word-splits                  (map (fn [i] (cond (> i spacethreshold) " SPACE " (> dashthreshold i) "" :else " ")) partial-diffs-between)
        morse-string                 (->> (concat word-splits [""])
                                          (interleave morse) ;; interleave morse dits and dashes with word separators and character separators
                                          (apply str))
        decoded                      (->> (clojure.string/split morse-string #" ")
                                          (map #(get morse-codes %) )
                                          (apply str))]
    decoded))

(defn -main [& args]
  (println (morse->text (first args))))

(def morse-codes {".-.-.-" \.,
                  "-----"  \0,
                  ".----"  \1,
                  "..---"  \2,
                  "...--"  \3,
                  "....-"  \4,
                  "....."  \5,
                  "-...."  \6,
                  "--..."  \7,
                  "---.."  \8,
                  "----."  \9,
                  ".-"     \A,
                  "-..."   \B,
                  "-.-."   \C,
                  "-.."    \D,
                  "."      \E,
                  "..-."   \F,
                  "--."    \G,
                  "...."   \H,
                  ".."     \I,
                  ".---"   \J,
                  "-.-"    \K,
                  ".-.."   \L,
                  "--"     \M,
                  "-."     \N,
                  "---"    \O,
                  ".--."   \P,
                  "--.-"   \Q,
                  ".-."    \R,
                  "..."    \S,
                  "-"      \T,
                  "..-"    \U,
                  "...-"   \V,
                  ".--"    \W,
                  "-..-"   \X,
                  "-.--"   \Y,
                  "--.."   \Z
                  "SPACE" \space})
