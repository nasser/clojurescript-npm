(ns clojurescript.core
  (:require [clojure.string :as string]
            [cljs.nodejs :as nodejs]
            [cljs.js :as cljsjs]
            [replumb.repl :as replumbrepl]
            [replumb.core :as replumb]
            [replumb.nodejs :as replumbjs]))

(nodejs/enable-util-print!)

;; Node file reading fns

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; From mfikes/elbow ;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fs (nodejs/require "fs"))
(def AdmZip (nodejs/require "adm-zip"))

(defn node-read-file
  "Accepts a filename to read and a callback. Upon success, invokes
  callback with the source. Otherwise invokes the callback with nil."
  [filename cb]
  (.readFile fs filename "utf-8"
    (fn [err source]
      (cb (when-not err
            source)))))

(defn node-read-file-sync
  "Accepts a filename to read. Upon success, returns the source.
  Otherwise returns nil."
  [filename]
  (.readFileSync fs filename "utf-8"))

;; Facilities for loading Closure deps

(def goog-path-root "out/goog/")

(defn closure-index
  []
  (let [paths-to-provides
        (map (fn [[_ path provides]]
               [path (map second
                       (re-seq #"'(.*?)'" provides))])
          (re-seq #"\ngoog\.addDependency\('(.*)', \[(.*?)\].*"
            (node-read-file-sync (str goog-path-root "deps.js"))))]
    (into {}
      (for [[path provides] paths-to-provides
            provide provides]
        [(symbol provide) (str goog-path-root (second (re-find #"(.*)\.js$" path)))]))))

(def closure-index-mem (memoize closure-index))

(defn load-goog
  [name cb]
  (if-let [goog-path (get (closure-index-mem) name)]
    (if-let [source (node-read-file-sync (str goog-path ".js"))]
      (cb {:source source
           :lang   :js})
      (cb nil))
    (cb nil)))

;; Facilities for loading files

(defn- filename->lang
  "Converts a filename to a lang keyword by inspecting the file
  extension."
  [filename]
  (if (string/ends-with? filename ".js")
    :js
    :clj))

(defn- read-some
  "Reads the first filename in a sequence of supplied filenames,
  using a supplied read-file-fn, calling back upon first successful
  read, otherwise calling back with nil."
  [[filename & more-filenames] read-file-fn cb]
  (if filename
    (read-file-fn
      filename
      (fn [source]
        (if source
          (cb {:lang   (filename->lang filename)
               :source source})
          (read-some more-filenames read-file-fn cb))))
    (cb nil)))

(defn- filenames-to-try
  "Produces a sequence of filenames to try reading, in the
  order they should be tried."
  [src-paths macros path]
  (let [extensions (if macros
                     [".clj" ".cljc"]
                     [".cljs" ".cljc" ".js"])]
    (for [extension extensions
          src-path src-paths]
      (str src-path "/" path extension))))

(defn skip-load?
  [name macros]
  ((if macros
     #{'cljs.pprint}
     #{'goog.object
       'goog.string
       'goog.string.StringBuffer
       'goog.array
       'clojure.string
       'clojure.set
       'cljs.core
       'cljs.env
       'cljs.reader
       'cljs.tagged-literals
       'cljs.tools.reader.impl.utils
       'cljs.pprint}) name))

;; An atom to keep track of things we've already loaded
(def loaded (atom #{}))

(defn load?
  [name macros]
  (let [do-not-load (or (@loaded name)
                      (skip-load? name macros)
                      (js/goog.getObjectByName (str name)))]
    (swap! loaded conj name)
    (not do-not-load)))

(defn make-load-fn
  "Makes a load function that will read from a sequence of src-paths
  using a supplied read-file-fn. It returns a cljs.js-compatible
  *load-fn*.

  Read-file-fn is a 2-arity function (fn [filename source-cb] ...) where
  source-cb is itself a function (fn [source] ...) that needs to be called
  with the source of the library (as string)."
  [src-paths read-file-fn]
  (fn [{:keys [name macros path]} cb]
    (if (load? name macros)
      (if (re-matches #"^goog/.*" path)
        (load-goog name cb)
        (read-some (filenames-to-try src-paths macros path) read-file-fn cb))
      (cb {:source ""
           :lang   :js}))))

(defn handle-result
  [{:keys [error value]}]
  (if-not error
    value
    (let [cause (.-cause error)
          description (.-description error)
          number (.-number error)
          fileName (.-fileName error)
          lineNumber (.-lineNumber error)
          columnNumber (.-columnNumber error)
          stack (.-stack error)
          message [(if cause cause)
                   (if description description)
                   (if (and fileName lineNumber columnNumber)
                     (str fileName ":" lineNumber "," columnNumber))
                   (if stack stack)]]
      (.error js/console
              (string/join "\n" (remove nil? message))))))

(defn node-module-paths
  "Implementation of the algorithm that
  Node uses to resolve modules"
  [base]
  (->> (string/split base "/")
       reverse
       (iterate rest)
       (take-while (comp not empty?))
       (map reverse)
       (map #(string/join "/" %))
       (map #(str % "/node_modules"))
       vec
       clj->js))

(defn jars-in-dir
  "Sequence of all .jar files in base"
  [base]
  (if (fs.existsSync base)
    (->> (fs.readdirSync base)
         (filter #(re-find #"\.jar" %))
         (map #(str base "/" %)))))

(defn node-module-paths-with-jars
  "Node load path interleaved with .jar files in node_module
  directories"
  [base]
  (as-> base %
        (node-module-paths %)
        (interleave % (map jars-in-dir %))
        (remove nil? %)
        (flatten %)))

(def ^:dynamic *load-paths*)

(defn establish-load-path
  ([] (establish-load-path (js/process.cwd)))
  ([base]
   (aset js/module "paths" (node-module-paths base))
   (set! *load-paths* (into [base] (node-module-paths-with-jars base)))))

;; TODO not sure if process.cwd is a good default
(establish-load-path)

(defn ^:export eval [in-str]
  (replumb/read-eval-call
    (merge
      {:no-pr-str-on-value true
       :init-fn! replumbjs/init-fn!}
      (replumb/options
        :node
        (make-load-fn
          *load-paths*
          (fn [filename source-cb]
            (source-cb
              (cond 
                (re-find #"\.jar/" filename)
                (let [[jar-path path] (string/split filename ".jar/")
                      zip (AdmZip. (str jar-path ".jar"))]
                  (if (.getEntry zip path)
                    (.readAsText zip path)))
                (.existsSync fs filename)
                (node-read-file-sync filename)))))))
    handle-result
    in-str))

(defn ^:export compile [in-str]
  (cljsjs/compile-str
    replumbrepl/st
    in-str ""
    (replumbrepl/base-eval-opts!)
    handle-result))

(set! *main-cli-fn* (fn []))