(ns todo-ddom.server
  (:require [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.http.route :as route]
            [ring.util.response :as ring-resp]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor.helpers :refer [defhandler]]
            [io.pedestal.log :as log]
            [todo-ddom.view :as view]))

(defhandler index [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body view/index})

(defroutes routes
  [[["/" {:get index}]]])

(def server-conf {::http/routes #(deref #'routes)
                  ::http/resource-path "/public"
                  ::http/type :immutant
                  ::http/port 8080})

(defonce server (http/create-server server-conf))

(comment

  (http/start server)
  (http/stop server)

  )
