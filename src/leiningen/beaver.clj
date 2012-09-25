(ns
  ^{:doc "This library is a plugin for generating parsers using Beaver in Leiningen."}
  leiningen.beaver
  (:require [leiningen.clean]
            [leiningen.compile]
            [clojure.string :as s]
            [clojure.java.io :as io])
  (:use [robert.hooke :only (add-hook)]
        [leiningen.clean :only (delete-file-recursively)])
  (:import [java.io File FileFilter]
           [java.util.regex Pattern]
           [JFlex Main]
           [beaver.comp ParserGenerator]
           [beaver.comp.io SrcReader]
           [beaver.comp.util Log]))

(def ^:dynamic *silently* false)
;; Cannot import Options since both beaver.com.run.Options and JFlex.Options are both needed

(def syntax-files ^{:doc "File extensions for J/Flex"} #{"jflex" "jlex" "lex" "flex"})

(def grammar-files ^{:doc "File extensions for Beaver grammars"} #{"g" "grammar"})

(defn- has-suffix?
  "Returns true if file f has one of the given suffixes as its filename extension.
   The suffixes should not include the '.' character."
  [suffixes f]
  (let [n (.getName f)
        idx (.lastIndexOf n ".")
        suffix (if (> idx -1) (.substring n (inc idx)))]
    (contains? suffixes suffix)))

(defn- files-of-type
    "List all files that are in the given directory with names ending in one of the given suffixes."
    [^File dir suffixes]
    (seq (.listFiles dir (proxy [FileFilter] [] (accept [f] (has-suffix? suffixes f))))))

(defmacro set-val
  "Macro for setting the value of a field on a Java object."
  [o sym val] `(set! (. ~o ~sym) ~val))

(defmacro set-bool
  "Macro for setting the value of a boolean field on a Java object based on a mapped value."
  [o field m k]
  `(set-val ~o ~field (get ~m ~k false)))

(defn- get-beaver-opts
  "Retrieves Beaver options from a Clojure map"
  [opts dest]
  (doto (beaver.comp.run.Options.)
    (set-bool report_actions opts :a)
    (set-bool no_compression opts :c)
    (set-bool no_output opts :D)
    (set-bool exp_parsing_tables opts :e)
    (set-bool name_action_classes opts :n)
    (set-bool sort_terminals opts :s)
    (set-bool terminal_names opts :t)
    (set-bool export_terminals opts :T)
    (set-bool use_switch opts :w)
    (set-val dest_dir dest)))

(def sepChar (. File separatorChar))

(defn- file-grep
  "Returns a seq of all lines in a file that match a pattern"
  [^Pattern re ^File file]
  (filter #(re-find re %) (line-seq (io/reader file))))

(defn determine-out-dir
  "Determines the output directory for a J/Flex file."
  [^File dest-dir ^File file]
  (letfn [(pkg [s] (.substring s 8 (max 8 (dec (count s)))))]
    (if-let [line (first (file-grep #"^package " file))]
      (File. (str dest-dir sepChar (s/replace (pkg (s/trim line)) \. sepChar)))
      dest-dir)))

(defn run-jflex
  "Runs the J/Fex scanner generator"
  [^File src-dir ^File dest-dir opts]
  (let [src-files (files-of-type src-dir syntax-files)]
    (if (empty? src-files) (println src-dir " has no grammar files")
      (do
        (if (get opts :verbose false) (set! JFlex.Options/verbose true))
        (doseq [^File file src-files]
          (if-not *silently* (println "Generating scanner from" (str file)))
          (JFlex.Options/setDir (determine-out-dir dest-dir file))
          (Main/generate file))))))

(defn run-beaver
  "Runs the Beaver parser generator"
  [^File src-dir ^File dest-dir opts]
  (let [src-files (files-of-type src-dir grammar-files)]
    (if (empty? src-files) (println src-dir " has no grammar files")
      (let [beaver-opts (get-beaver-opts opts dest-dir)
            log (Log.)]
        (doseq [file src-files]
          (if-not *silently* (println "Generating parser from" (str file)))
          (ParserGenerator/compile (SrcReader. file) beaver-opts log))))))

(defn beaver-src-dir
  "Find the source for the grammar and syntax for the project"
  [project]
  (File. (get project :grammar-src-dir "src/lang")))

(defn beaver-dest-dir
  "Find the destination for the grammar and syntax classes"
  [project]
  (File. (get project :grammar-dest-dir "target/gen-src")))

(defn beaver-options
  "Extract beaver relevant options from the project"
  [project]
  (get project :beaver-opts))

(defn compile-jflex-beaver
  "Run the J/Flex scanner generator and the Beaver parser generator"
  [^File src-dir ^File dest-dir beaver-opts]
  (if-not (.isDirectory src-dir)
    (println src-dir " is not a directory")
    (let [beaver-opts (assoc beaver-opts :d dest-dir)]
      (run-jflex src-dir dest-dir beaver-opts)
      (run-beaver src-dir dest-dir beaver-opts))))

(defn beaver
  "Main entry point to compile J/Flex syntax and Beaver grammar files"
  [project]
  (compile-jflex-beaver (beaver-src-dir project)
                        (beaver-dest-dir project)
                        (beaver-options project)))

(defn clean-beaver-hook
  "Clean the Beaver and J/Flex output"
  [f & [project & _ :as args]]
  (apply f args)
  (delete-file-recursively (beaver-dest-dir project) true))

(defn plugin-compile-hook
  "Print out what's happening"
  [f & [project & _ :as args]]
  (println "Running compile")
  (apply f args))

;; Add a hook to the "lein clean" task to clean the target directory
(defn hooks []
  (add-hook #'leiningen.clean/clean clean-beaver-hook)
  (add-hook #'leiningen.compile/compile plugin-compile-hook))

