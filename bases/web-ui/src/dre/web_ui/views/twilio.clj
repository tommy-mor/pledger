(ns dre.web-ui.views.twilio
  (:require [hato.client :as hc]
            [ring.util.codec :as codec]
            [com.rpl.specter :as s])
  (:import java.util.Base64))

(defn encode [to-encode]
  (String. (.encode (Base64/getEncoder) (.getBytes to-encode))))

(def auth-headers {"Authorization" (str "Basic " (encode (str
                                                          (-> "secrets.edn" slurp read-string :twilio/account-sid)
                                                          ":"
                                                          (-> "secrets.edn" slurp read-string :twilio/auth-token))))
                   "Content-type" "application/x-www-form-urlencoded"})

(defn send-verification [phone-number !done]
  (hc/post (str "https://verify.twilio.com/v2/Services/" (-> "secrets.edn" slurp read-string :twilio/service-id) "/Verifications")
           {:headers auth-headers
            :body (codec/form-encode {:To phone-number
                                      :Channel "sms"})})
  (reset! !done :confirm))

(defn send-confirmation [phone-number code !done]
  (def r (hc/post (str "https://verify.twilio.com/v2/Services/" (-> "secrets.edn" slurp read-string :twilio/service-id) "/VerificationCheck")
                  {:headers auth-headers
                   :body (codec/form-encode {:To phone-number
                                             :Code code})}))
  (reset! !done :done))

(defn trim-whitespace [st]
  (apply str (reverse (s/transform [(s/filterer #(Character/isWhitespace %))] s/NONE st))))

(comment
  (trim-whitespace "+1 123 456 7890"))


