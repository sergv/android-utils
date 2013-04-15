(defproject android-utils/android-utils "0.4.0"
  :description "Various utilities for developing for Android in Clojure."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"

  :warn-on-reflection true

  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options     ["-target" "1.6" "-source" "1.6"]

  ;; :local-repo "/home/sergey/.m2/repository/"
  :repositories [["local" "file:///home/sergey/.m2/repository/"]]
  :plugins [[lein-droid "0.1.0-preview2-enhanced-dex"]]
  :dependencies [[android/clojure "1.5.0"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.nrepl "0.2.1"]]
                   :android {:aot :all-with-unused}}
             :release {:android {:aot :all}}}
  :android {:sdk-path "/home/sergey/projects/android/android-sdk-linux"})

;; Local Variables:
;; clojure-compile/lein-command: "lein with-profiles %s do compile, install"
;; End:
