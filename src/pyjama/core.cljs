(ns pyjama.core
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as client]
            [cljs.core.async :refer [<!] :as async]
            [goog.string :as gstring]
            [goog.string.format]
            [goog.json :as gjson]
            ))

(defn print-tokens [parsed key]
  (when-let [resp (get-in parsed key)]
    (println resp)))

(defn print-create-tokens [parsed]
  (print-tokens parsed [:status]))

(defn print-pull-tokens [parsed]
  (print-tokens parsed [:status]))

(defn print-generate-tokens [parsed]
  (print-tokens parsed [:response]))

(defn print-chat-tokens [parsed]
  (print-tokens parsed [:message :content]))

(defn pipe-tokens [ch json-path parsed]
  (when-let [resp (get-in parsed json-path)]
    (async/go (async/>! ch resp))))

(defn pipe-generate-tokens [ch parsed]
  (pipe-tokens ch [:response] parsed))

(defn pipe-chat-tokens [ch parsed]
  (pipe-tokens ch [:message :content] parsed))

(defn pipe-pull-tokens [ch parsed]
  (pipe-tokens ch [:status] parsed))

(defn structure-to-edn [body]
  (-> body :response js/JSON.parse (js->clj :keywordize-keys true)))

(def DEFAULTS
  {
   :generate
   [{:model      "llama3.2"
     :keep_alive "5m"
     :stream     false
     :images     []}
    :post
    print-generate-tokens]

   :tags
   [{} :get identity]

   :show
   [{:model   "llama3.2"
     :verbose false}
    :post
    identity]

   :pull
   [{:model  "llama3.2"
     :stream false}
    :post
    identity]

   :ps
   [{} :get identity]

   :create
   [{} :post identity]

   :delete
   [{} :delete identity]

   :chat
   [{:model      "llama3.2"
     :keep_alive "5m"
     :stream     false
     :messages   [{:role :user :content "why is the sky blue?"}]}
    :post
    print-chat-tokens]

   :embed
   [{:model "all-minilm"}
    :post
    :embeddings]

   :version
   [{} :get identity]})

(defn templated-prompt [input]
  (if (contains? input :pre)
    (let [update {:prompt
                  (if (vector? (:prompt input))
                    (apply gstring/format (:pre input) (:prompt input))
                    (gstring/format (:pre input) (:prompt input)))}]
      (merge (dissoc input :pre) update))
    input))

(defn ollama
  ([url command]
   (ollama url command {}))
  ([url command input]
   (assert (contains? (set (keys DEFAULTS)) command)
           (str "Invalid command: " command))
   (ollama url command input (-> command DEFAULTS last)))
  ([url command input _fn]
   (go
     (let [cmd-params (command DEFAULTS)
           params (merge (first cmd-params) (templated-prompt input))
           options {
                    :method  (clojure.string/upper-case (name (nth cmd-params 1)))
                    :url     (str url "/api/" (name command))
                    :headers {"Content-Type" "application/json"}
                    :body    (gjson/serialize (clj->js params))
                    }
           response (<! (client/request options))
           body (_fn (js->clj (:body response) :keywordize-keys true))
           ]
       (prn "> " body)
       body))))
