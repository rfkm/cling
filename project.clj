(defproject rkworks/cling "0.1.3"
  :description "A Clojure CLI applications library."
  :url "https://github.com/rfkm/cling"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :deploy-repositories [["snapshots" {:url "https://clojars.org/repo/"
                                      :username [:gpg :env]
                                      :password [:gpg :env]}]
                        ["releases" {:url "https://clojars.org/repo/"
                                     :creds :gpg}]]
  :dependencies [[org.clojure/tools.cli "0.3.3"]
                 [io.aviso/pretty "0.1.24"]
                 [prismatic/plumbing "0.5.2"]
                 [potemkin "0.4.3"]
                 [slingshot "0.12.2"]]
  :profiles {:dev    {:dependencies [[org.clojure/clojure "1.7.0"]
                                     [midje "1.8.3"]]
                      :plugins      [[lein-midje "3.2"]
                                     [lein-environ "1.0.2"]]
                      :env          {:env "dev"}}
             :1.6    {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7    {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8    {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :master {:dependencies [[org.clojure/clojure "1.9.0-master-SNAPSHOT"]]}}
  :aliases {"all" ["with-profile" "+1.6:+1.7:+1.8:+master"]})
