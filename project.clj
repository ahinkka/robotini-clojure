(defproject robotini-clojure "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "1.0.0"]
                 [amalloy/ring-buffer "1.3.1"]
                 [com.alexdupre/pngj "2.1.2.1"]
                 [org.boofcv/boofcv-core "0.37"]
                 [ring/ring-core "1.9.2"]
                 [http-kit "2.5.3"]]
  :repl-options {:init-ns robotini-clojure.core}
  :main robotini-clojure.core
  :aot :all)
