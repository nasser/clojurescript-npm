(ns clojurescript.core
  (:require [cljs.pprint :refer [pprint]]
            [cljs.js :as cljs]
            [cljs.tools.reader :as r]))

(enable-console-print!)

(def ^:export read r/read)

(def ^:export readString r/read-string)

(defn ^:export eval [in-str]
  (cljs/eval-str (cljs/empty-state)
                 in-str 'bar
                 {:eval cljs/js-eval
                  :context :expr
                  :def-emits-var true}
                 (fn [{:keys [error value]}]
                   (if-not error
                     value
                     (do
                       (.error js/console error))))))

(defn ^:export compile [in-str]
  (cljs/compile-str (cljs/empty-state)
                    in-str 'bar
                    {:eval cljs/js-eval
                     :context :expr
                     :def-emits-var true}
                    (fn [{:keys [error value]}]
                      (if-not error
                        value
                        (do
                          (.error js/console error))))))

(set! *main-cli-fn*
      (fn []))