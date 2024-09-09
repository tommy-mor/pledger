(ns dre.web-ui.views.twilio
  (:require [hato.client :as hc]
            [ring.util.codec :as codec])
  (:import java.util.Base64))

(defn encode [to-encode]
  (String. (.encode (Base64/getEncoder) (.getBytes to-encode))))

(def auth-headers {"Authorization" (str "Basic " (encode (str
                                                          (-> "secrets.edn" slurp read-string :twilio/account-sid)
                                                          ":"
                                                          (-> "secrets.edn" slurp read-string :twilio/auth-token))))
                   "Content-type" "application/x-www-form-urlencoded"})

(defn send-verification [phone-number]
  (hc/post (str "https://verify.twilio.com/v2/Services/" (-> "secrets.edn" slurp read-string :twilio/service-id) "/Verifications")
           {:headers auth-headers
            :body (codec/form-encode {:To phone-number
                                      :Channel "sms"})}))
