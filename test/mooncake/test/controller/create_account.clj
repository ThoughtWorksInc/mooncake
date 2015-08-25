(ns mooncake.test.controller.create-account
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [mooncake.controller.create-account :as cac]
            [mooncake.db.mongo :as mongo]
            [mooncake.db.user :as user]
            [mooncake.routes :as routes]
            [mooncake.test.test-helpers.enlive :as eh]))

(facts "about show-create-account"
       (let [show-create-account-request (-> (mock/request :get (routes/absolute-path {} :show-create-account))
                                             (assoc :context {:translator {}}))]
         (fact "when auth-provider-user-id is in the session it should render the create-account page"
                (let [response (-> (assoc show-create-account-request :session {:auth-provider-user-id
                                                                                ...user-id...})
                                   cac/show-create-account)]
                  response => (eh/check-renders-page :.func--create-account-page)))

         (fact "when auth-provider-user-id is not in the session navigating to create-account should redirect to /sign-in"
               (cac/show-create-account show-create-account-request) =>
               (eh/check-redirects-to (routes/absolute-path {} :sign-in)))))

(defrecord MemoryDatabase [data]
  mongo/Database
  (fetch [this coll k]
    (@data k))
  (find-item [this coll query-m]
    (some #(when (clojure.set/subset? (set query-m) (set %)) %) (vals @data)))
  (store! [this coll key-param item]
    (do
      (swap! data assoc (key-param item) item)
      item)))

(defn create-in-memory-db
  ([] (create-in-memory-db {}))
  ([data] (MemoryDatabase. (atom data))))

(facts "about create-account"
       (facts "when successful"
              (let [create-account-request {:params {:username "username"}
                                            :session {:auth-provider-user-id ...user-id...}}
                    db (create-in-memory-db)
                    response (cac/create-account db create-account-request)]
                (fact "it should create the user"
                      (user/fetch-user db ...user-id...) => {:auth-provider-user-id ...user-id...
                                                            :username "username"})
                (fact "it should redirect to /"
                      response => (eh/check-redirects-to (routes/absolute-path {} :index)))
                (fact "it should set the username in the session"
                      (get-in response [:session :username]) => "username")
                (fact "it should remove the auth-provider-user-id from the session"
                      (get-in response [:session :auth-provider-user-id]) => nil?)))

       (fact "missing auth-provider-user-id in session redirects to /sign-in and nothing is stored"
             (let [create-account-request {:params {:username "username"}}
                   response (cac/create-account ...store... create-account-request)]
               response => (eh/check-redirects-to (routes/absolute-path {} :sign-in))
               (provided
                 (user/create-user! anything anything anything) => ...never-called... :times 0)))

       (fact "invalid username parameter renders show-create-account and nothing is stored"
             (let [create-account-request {:params {:username "!!*FAIL*!!"}
                                           :session {:auth-provider-user-id ...user-id...}
                                           :context {:translator {}}}
                   response (cac/create-account ...store... create-account-request)]
               response => (eh/check-renders-page :.func--create-account-page)
               (:body response) => (contains "clj--username__validation")
               (:body response) => (contains "!!*FAIL*!!")
               (provided
                 (user/create-user! anything anything anything) => ...never-called... :times 0)))

       (fact "duplicate username parameter renders show-create-account and nothing is stored"
             (let [create-account-request {:params {:username "dupe_username"}
                                           :session {:auth-provider-user-id ...user-id...}
                                           :context {:translator {}}}
                   store (create-in-memory-db {"some-id" {:auth-provider-user-id "some-id"
                                                          :username "dupe_username"}})
                   response (cac/create-account store create-account-request)]
               response => (eh/check-renders-page :.func--create-account-page)
               (:body response) => (contains "clj--username__validation")
               (:body response) => (contains "dupe_username")
               (provided
                 (user/create-user! anything anything anything) => ...never-called... :times 0))))

(facts "about is-username-duplicate?"
      (fact "when username is unique returns false"
            (let [store (create-in-memory-db)]
              (cac/is-username-duplicate? store "unique_username")) => false)

      (fact "duplicate username returns true"
            (let [store (create-in-memory-db {"some-id" {:auth-provider-user-id "some-id"
                                                         :username "dupe_username"}})]
              (cac/is-username-duplicate? store "dupe_username")) => true))
