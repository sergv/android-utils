(defproject android-utils/android-utils "0.6.0"
  :description "Various utilities for developing for Android in Clojure."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"

  :warn-on-reflection true

  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6"]
  :target-path "bin"
  :compile-path "bin/classes"

  ;; :local-repo "/home/sergey/.m2/repository/"
  :repositories [["local" "file:///home/sergey/.m2/repository/"]]
  :plugins [[lein-droid "0.1.0-preview5-enhanced"]]
  :dependencies [[android/clojure "1.5.0"]
                 ;; [org.clojure-android/clojure "1.5.1"]
                 [neko/neko "2.0.0-beta1"]
                 [android-utils/android-java-utils "0.1.0"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.nrepl "0.2.1"]]}
             :release {:omit-source true}}
  :android {:sdk-path "/home/sergey/projects/android/android-sdk-linux"
            :libraty true

            :min-version "10"
            :target-version "15"
            :aot :all
            :aot-exclude-ns ["clojure.parallel" "clojure.core.reducers"]})

;; Local Variables:
;; clojure-compile/lein-command: "LEIN_JAVA_CMD=/usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java lein with-profiles %s do droid compile, install"
;; End:
