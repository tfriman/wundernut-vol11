(ns wundernut.analyzer
  (:require [dynne.sampled-sound :refer :all]))

;; https://github.com/candera/dynne

;; message.wav: RIFF (little-endian) data, WAVE audio, Microsoft PCM, 16 bit, mono 44100 Hz

(def msg (read-sound "message.wav"))

;;(visualize msg)

(def message-chunks (chunks msg 44100))

;;(def f1 (trim msg 0 1))
;;(play f1)
;;(visualize f1)

;;(def f1-chunks (chunks f1 44100))

;;(apply + (map #(count (first %)) f1-chunks))
;; (def freqs (frequencies (flatten (apply concat (map first f1-chunks)))))

(defn- find-highs
  "Find indices of those going over the treshold and then getting down again"
  [snd treshold]
  (reduce (fn [a [idx v]]
            (let [elems (:elems a)
                  up? (:up a)]
              (if up?
                (if (> treshold v)
                  {:up false
                   :elems (conj elems idx)
                   }
                  a
                  )
                (if (> v treshold)
                  {:up true
                   :elems (conj elems idx)
                   }
                  a
                  ))))
          {:up false
           :elems []}
          (keep-indexed (fn [idx itm] [idx itm]) snd)))

;;(def hi (find-highs-obsolete (flatten (apply concat (map first f1-chunks))) 0.21))

;;(def one-sec (flatten (apply concat (map first f1-chunks))))

(def all (flatten (apply concat (map first message-chunks))))

(def x (:elems (find-highs (map #(apply max %) (partition 441 all)) 0.11)))

(def partial-diffs (map (fn [[a b]] (- b a)) (partition 2 x)))

(def partial-diffs-between (map (fn [[a b]] (- b a)) (partition 2 (rest x))))

;;(frequencies partial-diffs-between)
;; {6 81, 20 16, 19 24, 47 2, 46 10, 5 3}

(def word-splits (map (fn [v] (if (> 17 v) "" " ")) partial-diffs-between))

(def morse (map (fn [v] (if (> 12 v) "." "-")) partial-diffs))

;;(visualize (trim msg 3.5 7))

;;(visualize (trim msg 4.5 5.6))
(def morse-string (apply str (interleave morse (lazy-cat word-splits [""]))))

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
                  "--.."   \Z})

(apply str (map #(get morse-codes %) (clojure.string/split morse-string #" ")))

;; "MAYWETOGETHERBECOMEGREATERTHANTHESUMOFBOTHOFUS.SAREK."

;; I am pleased to see that we have differences. May we together become greater than the sum of both of us.
;; ... Surak of Vulcan, "The Savage Curtain," stardate 5906.4..
