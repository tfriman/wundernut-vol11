(ns wundernut.analyzer
  (:require [dynne.sampled-sound :refer [read-sound chunks]])
  (:gen-class))

;; Decoded wav files containing Morse code
;; expected file format: RIFF (little-endian) data, WAVE audio, Microsoft PCM, 16 bit, mono 44100 Hz

(declare morse-codes)

(defn- find-highs
  "Find indices of those going over the treshold and then getting down again"
  [snd treshold]
  (reduce (fn [a [idx v]]
            (let [elems (:elems a)
                  up? (:up a)]
              ;; TODO condp? joku muu struktuuri kuin ifelse
              (if up?
                (if (> treshold v)
                  {:up false
                   :elems (conj elems idx)}
                  a)
                (if (> v treshold)
                  {:up true
                   :elems (conj elems idx)}
                  a))))
          {:up false
           :elems []}
          (keep-indexed (fn [idx itm] [idx itm]) snd)))

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

(def remove-consecutive (->consecutive-transducer
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

(defn morse->text
  "Parse input file path wav file to text string"
  [input-wav-path]
  ;; todo mieti josko tän voisi tehdä threadeten nätimmin
  (let [input-sound (read-sound input-wav-path)
        message-chunks (chunks input-sound 44100)
        amplitudes (flatten (apply concat (map first message-chunks)))
        highs (:elems (find-highs (map #(apply max %) (partition 441 amplitudes)) 0.11))
        partial-diffs (map (fn [[a b]] (- b a)) (partition 2 highs))
        _ (def partial-diffs partial-diffs)
        partial-diffs-between (map (fn [[a b]] (- b a)) (partition 2 (rest highs)))
        _ (def partial-diffs-between partial-diffs-between)
        [dashtreshold spacetreshold] (transduce non-consecutive-average conj (sort (keys (frequencies partial-diffs-between))))
        word-splits (map (fn [v] (if (> v spacetreshold) " SPACE " (if  (> dashtreshold v) "" " "))) partial-diffs-between)
        morse (map (fn [v] (if (> 12 v) "." "-")) partial-diffs)
        morse-string (apply str (interleave morse (lazy-cat word-splits [""])))
        decoded (apply str (map #(get morse-codes %) (clojure.string/split morse-string #" ")))]
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
