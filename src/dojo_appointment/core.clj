(ns dojo-appointment.core
  (:require
   [clojure.java.io :as jio]
   [clojure.string :as s]
   [clojure.data.xml :as x]
   [clojure.spec.alpha :as spec]
   [spec-tools.spec :as spect]
   [org.httpkit.server :as server]
   ;;[liberator.core :refer [resource defresource]]
   [ring.middleware.params :refer [wrap-params]]
   ;;[compojure.core :refer [defroutes ANY GET PUT POST]]
   [compojure.api.sweet :refer :all]))

(defprotocol TableRow
  (getrow [this]))

(defrecord Slot [start end]
  TableRow
  (getrow [this]
    (x/element :tr {}
      [(x/element :td {} start)
       (x/element :td {} end)
       (x/element :td {}
         (x/element :form {:method "post" :action "/bookapt"}
           (x/element :input {:type "text" :name "name"})
           (x/element :input {:type "hidden" :name "begin" :value start})
           (x/element :input {:type "hidden" :name "end" :value end})
           (x/element :input {:type "submit" :value "Book"})))])))

(defrecord Booking [slot name]
  TableRow
  (getrow [this]
    (let [{:keys [start end]} slot]
      (x/element :tr {}
        [(x/element :td {} start)
         (x/element :td {} end)
         (x/element :td {} name)]))))

(defrecord BookingSystem [free booked])

(defn init [min max step]
  (let [free (set (map (partial apply ->Slot) (partition 2 (range min max step))))]
    (->BookingSystem free #{})))

(def booksys (ref (init 0 100 5)))

(defn with-new [{:keys [free booked]} start end name]
  (let [slot (->Slot start end)
        free (:free @booksys)]
    (if-let [choosen (get free slot)]
      (let [updfree (disj free choosen)
            updbooked (conj booked (->Booking choosen name))]
        (->BookingSystem updfree updbooked))
      (throw (Exception. "No such slot exists")))))

(defn book-apt [start end name]
  (if (not (empty? name))
    (dosync
      (alter booksys with-new start end name))
    (throw  (Exception. "Name was empty"))))

(def headers [:h1 :h2 :h3 :h4 :h5 :h6])

(defn elemheader
  ([text]
   (elemheader 1 text))
  ([level text]
   (x/element (nth headers level) {} text)))

(defn elempara [text]
  (x/element :p {} text))

(defn htmldoc [title & content]
  (x/element :html {}
    (x/element :head {}
      (x/element :title {} title))
    (x/element :body {}
      content)))

(defn elem-table [header rows]
  "Takes header (list of strings) and rows (list of arrays of string elements)
and outputs a html table element using these."
  (x/element :table {}
    (x/element :tr {}
      (map #(x/element :th {} %) header))
    (map getrow rows)))

(spec/def ::name spect/string?)

(def app
  (context "/" []
           :coercion :spec
           (GET "/" []
                (x/emit-str
                 (htmldoc "Appointment Booking System"
                          (elemheader "Main menu:")
                          (elempara
                           (x/element :a {:href "/slots"} "View available free slots"))
                          (elempara
                           (x/element :a {:href "/bookings"} "View all existing bookings")))))
           (GET "/slots" []
                (x/emit-str
                 (htmldoc "Appointment Free Slots"
                          (elemheader "Book free slots:")
                          (elem-table ["Begin" "End" "Name"]
                                      (sort-by :start (:free @booksys)))
                          (elempara
                           (x/element :a {:href "/bookings"} "View all existing bookings")))))
           (GET "/bookings" []
                (x/emit-str
                 (htmldoc "Booked Appointments"
                          (elemheader "Booked Appointments table:")
                          (elem-table ["Start" "End" "Name"]
                                      (sort-by (comp :start :slot) (:booked @booksys)))
                          (elempara
                           (x/element :a {:href "/slots"} "Book another appointment")))))
           (POST "/bookapt" [begin end name]
                 :return ::name
                 (try
                   (book-apt (Integer. begin) (Integer. end) name)
                   (x/emit-str
                    (htmldoc "Appointment Booking"
                             (elempara
                              "Appointment successfully booked!")
                             (elempara
                              (x/element :a {:href "/bookings"} "View all booked appointments"))
                             (elempara
                              (x/element :a {:href "/slots"} "Book another free slot"))))
                   (catch Exception x
                     (x/emit-str
                      (htmldoc "Appointment Booking Error"
                               (elempara
                                "Something went wrong!")
                               (elempara
                                (.getMessage x))
                               (elempara
                                (x/element :a {:href "/slots"} "Try booking another free slot")))))))))

(def handler
  (-> app wrap-params))
