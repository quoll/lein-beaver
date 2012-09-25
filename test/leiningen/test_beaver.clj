(ns leiningen.test-beaver
  (:use [leiningen.beaver :only (beaver clean-beaver-hook)]
        [leiningen.clean :only (delete-file-recursively)]
        [clojure.test])
  (:import [java.io File]))

(def beaver-out-dir "beaver-out")
(def beaver-project {:grammar-src-dir "beaver" :grammar-dest-dir beaver-out-dir})
(def sc (. File separatorChar))

(when (.exists (File. beaver-out-dir)) (delete-file-recursively beaver-out-dir false))

(defn out-file-exists [f] (.exists (File. (str beaver-out-dir sc "expr" sc "eval") f)))

(deftest test-beaver-compile
  (let [result (with-out-str (beaver beaver-project))]
    (println "result: " result)
    (is (true? (out-file-exists "ExpressionScanner.java")))
    (is (true? (out-file-exists "ExpressionParser.java")))
    ))

(deftest test-clean
  (let [result (with-out-str (clean-beaver-hook (fn [& _]) beaver-project))]
    (is (false? (out-file-exists "ExpressionParser.java")))
    (is (false? (out-file-exists "ExpressionScanner.java")))))

