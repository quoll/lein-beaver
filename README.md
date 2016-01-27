# lein-beaver

A Leiningen plugin to wrap the Beaver compiler generator and the JFlex lexer generator.

## Usage

Put [![Clojars Project](https://img.shields.io/clojars/v/org.clojars.kostafey/lein-beaver.svg)](https://clojars.org/org.clojars.kostafey/lein-beaver)
into the `:plugins` vector of your project.clj.

The beaver task can be invoked directly as a target:

`$ lein beaver`

It can also be chained into the compile chain by prepending beaver to the default :prep-tasks
property of the project. This leads to:

`:prep-tasks ["beaver" "javac" "compile"]`

### Options

The following properties are used by the plugin (and ignored by the rest of Leiningen):

  `:grammar-src-dir` - A string containing the (usually relative) directory path where the
  grammar and syntax files are found. Defaults to `src/lang`.

  `:grammar-dest-dir` - A string containing the (usually relative) directory path where the
  output .java files are placed. Defaults to `target/gen-src`. Be sure to include this in
  the `:java-source-paths` vector so that you will compile the output when `javac` gets run.

### Beaver Options

Beaver options can be set by setting the `:beaver-opts` property to a map of options. The
options available are all based on the normal Beaver command line options, described at:

  `http://beaver.sourceforge.net/how2run.html`

Each flag should be converted to a keyword (just be placing a : in front of the flag name,
maintaining case). On/off flags should be set to a boolean value.

For instance, to generate a separate file for Beaver terminals (the -T flag) and to sort
those terminals (-s), this is done by adding the following to project.clj:

`:beaver-opts { :T true, :s true }`

## TODO

Tasks are always run, rather than detecting when the source files are more recent than the
destination file.

Also, by default JFlex always creates classes with "default" visibility. This is OK if your
Clojure code is in the same package, but recompiles without cleaning can get confused with
the JFlex generated class not being rebuilt in the `javac` operation. The result is that
the newly generate Clojure class cannot link to the existing JFlex generated class. The fix
is to just run clean and recompile. Alternatively, add the `%public` option to the JFlex
lexer definition file.

## Example

The following is a simple project.clj file for building a parser:

`(defproject turtle "0.1.0-SNAPSHOT"
    :description "A parser for Foo"
    :url "http://example.com/Foo"
    :license {:name "Eclipse Public License"
    :url "http://www.eclipse.org/legal/epl-v10.html"}
    :dependencies [[org.clojure/clojure "1.4.0"]
                   [net.sf.beaver/beaver-ant "0.9.9"]]

    :plugins [[lein-beaver "0.1.2-SNAPSHOT"]
              [lein-pprint "1.1.1"]]  ;; good to have to see what the default project is

    :prep-tasks ["beaver" "javac" "compile"]

    :source-paths ["src/main/clj"]  ;; Maven-style directory structure
    :test-paths ["src/test/clj"]
    :java-source-paths ["src/main/java" "target/src"]

    :grammar-src-dir "src/main/grammar"
    :grammar-dest-dir "target/src/"

    :main foo.core)

## License

Copyright © 2012 Paul Gearon

Distributed under the Eclipse Public License, the same as Clojure.
