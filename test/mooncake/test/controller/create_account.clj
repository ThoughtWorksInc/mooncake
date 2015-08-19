(ns mooncake.test.controller.create-account
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [ring.mock.request :as mock]
            [mooncake.controller.create-account :as cac]
            [mooncake.db.mongo :as mongo]
            [mooncake.routes :as routes]
            [mooncake.test.test-helpers :as th]))

(facts "about show-create-account"
       (let [show-create-account-request (-> (mock/request :get (routes/absolute-path {} :show-create-account))
                                             (assoc :context {:translator {}}))]
         (fact "when auth-provider-user-id is in the session it should render the create-account page"
                (let [response (-> (assoc show-create-account-request :session {:auth-provider-user-id 
                                                                                ...user-id...})
                                   cac/show-create-account)]
                  response => (th/check-renders-page :.func--create-account-page)))

         (fact "when auth-provider-user-id is not in the session navigating to create-account should redirect to /sign-in"
               (cac/show-create-account show-create-account-request) => 
               (th/check-redirects-to (routes/absolute-path {} :sign-in)))))

(defrecord MemoryStore [data]
  mongo/Database
  (fetch [this k]
    (@data k))
  (store! [this key-param item]
    (do
      (swap! data assoc (key-param item) item)
      item)))

(defn create-memory-store 
  ([] (create-memory-store {}))
  ([data] (MemoryStore. (atom data))))

(facts "about create-account"
       (fact "should store username and redirect to sign-in"
             (let [create-account-request {:params {:username ...username...}
                                           :session {:auth-provider-user-id ...user-id...}}
                   store (create-memory-store)
                   response (cac/create-account store create-account-request)]
               (mongo/fetch store ...user-id...) => {:auth-provider-user-id ...user-id...
                                                     :username ...username...}
               response => (th/check-redirects-to (routes/absolute-path {} :sign-in))))

       (fact "missing auth-provider-user-id in session redirects to /sign-in and nothing is stored"
             (let [create-account-request {:params {:username ...username...}} 
                   response (cac/create-account ...store... create-account-request)] 
               response => (th/check-redirects-to (routes/absolute-path {} :sign-in))
               (provided 
                 (mongo/create-user! anything anything anything) => ...never-called... :times 0)))

       ;; RS + RP - 18/08/15 - This will become a check for any validation errors
       (fact "missing username parameter renders show-create-account and nothing is stored"
             (let [create-account-request {:params nil
                                           :session {:auth-provider-user-id ...user-id...}
                                           :context {:translator {}}}
                   response (cac/create-account ...store... create-account-request)]
               response => (th/check-renders-page :.func--create-account-page)  
               (provided 
                 (mongo/create-user! anything anything anything) => ...never-called... :times 0))))
