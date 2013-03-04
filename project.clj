(defproject android-utils/android-utils "0.1.0"
  :description "Various utilities for developing for Android in Clojure."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"

  :warn-on-reflection true

  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]

  ;; :local-repo "/home/sergey/.m2/repository/"
  :repositories [["local" "file:///home/sergey/.m2/repository/"]]
  :dependencies [[android/clojure "1.4.0-android"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.nrepl "0.2.1"]]}})
