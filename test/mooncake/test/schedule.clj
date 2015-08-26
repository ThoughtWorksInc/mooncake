(ns mooncake.test.schedule
  (:require [midje.sweet :refer :all]
            [mooncake.schedule :as s]
            [clj-time.core :as time]))

(fact "check that a function can be scheduled to run every x seconds"
      (let [counter (atom 0)
            f (fn [time] (swap! counter inc))
            interval 1
            interval-millis (* 1000 interval)
            a-bit-more 100]
        (s/schedule f interval) => anything
        (do (Thread/sleep interval-millis))
        (Thread/sleep a-bit-more)
        @counter => 1
        (do (Thread/sleep interval-millis))
        (Thread/sleep a-bit-more)
        @counter => 2))

(defn add-seconds [date-time]
  (fn [seconds]
    (time/plus date-time (time/seconds seconds))))

(fact "check that correct interval list is generated"
      (let [now (time/now)]
        (take 5 (s/get-intervals 2)) => (map (add-seconds now) (range 0 10 2))
        (provided (time/now) => now)))

