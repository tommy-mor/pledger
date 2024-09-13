(ns dre.web-ui.views.twilio
  (:require [hato.client :as hc]
            [ring.util.codec :as codec]
            [com.rpl.specter :as s]
            [clojure.data.json :as json])
  (:import java.util.Base64))

(defn encode [to-encode]
  (String. (.encode (Base64/getEncoder) (.getBytes to-encode))))

(def secrets (-> "secrets.edn" slurp read-string))

(def auth-headers {"Authorization" (str "Bearer " (-> secrets :postack/api-key))
                   "Content-type" "application/json"})

(defn send-verification [phone-number !done]
  (let [r (hc/post "https://api.postack.dev/v1/verifications/sms"
                   {:headers auth-headers
                    :body (json/write-str {:to phone-number
                                           :verification_profile_id (:postack/verification-id secrets)
                                           :code (format "%06d" (rand-int 100000))})})]
    (reset! !done :confirm)
    (-> r :body (json/read-str {:key-fn keyword}) :verification :id)))

(defn send-confirmation [id phone-number code !done]
  (let [ r (-> (hc/post (str "https://api.postack.dev/v1/verifications/" id "/confirm")
                        {:headers auth-headers
                         :body (json/write-str {:code code})})
               :body
               (json/read-str {:key-fn keyword})
               :code)]
    (if (= r "verified")
      (reset! !done :done)
      (throw (ex-info "failed confirmation" {:code r})))))

(defn trim-whitespace [st]
  (apply str (reverse (s/transform [(s/filterer #(Character/isWhitespace %))] s/NONE st))))

(comment
  (trim-whitespace "+1 123 456 7890"))


