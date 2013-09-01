
(use '[clojure.java.io :only (file)])

(def project-key-def-file "project-key.clj")

(def key-def-info
  (let [f (file project-key-def-file)]
    (if (.exists f)
      (read-string (slurp project-key-def-file))
      nil)))

(defproject android-utils/android-utils "0.7.0"
  :description "Various utilities for developing for Android in Clojure."
  :url "https://github.com/sergv/android-utils"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"

  :warn-on-reflection true

  :source-paths ["src/clojure" "src/java"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6"]
  :target-path "bin"
  :compile-path "bin/classes"

  :repositories [["local" "file://~/.m2/repository/"]]
  :plugins [[lein-droid "0.1.0"]]
  :dependencies [[android/clojure "1.5.0"]
                 [neko/neko "2.0.0-beta3"]
                 [android-utils/android-java-utils "0.1.0"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.nrepl "0.2.1"]]}}
  :android ~(merge
             {:libraty true

              :min-version "10"
              :target-version "15"
              :aot :all
              :aot-exclude-ns ["clojure.parallel" "clojure.core.reducers"]}
             key-def-info))

;; Local Variables:
;; clojure-compile/lein-command: "LEIN_JAVA_CMD=/usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java lein with-profiles %s do droid compile, install"
;; End:
