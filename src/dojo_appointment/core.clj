(ns dojo-appointment.core
  (:require
   [clojure.java.io :as jio]
   [clojure.string :as s]
   [clojure.data.xml :as x]
   [org.httpkit.server :as server]
   [liberator.core :refer [resource defresource]]
   [ring.middleware.params :refer [wrap-params]]
   [compojure.core :refer [defroutes ANY GET PUT POST]]))

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

(def appts (ref (init 0 100 5)))

(defn getbookings []
  (:booked @appts))

(defn getfreeslots []
  (:free @appts))

(defn with-new [{:keys [free booked]} start end name]
  (let [slot (->Slot start end)
        free (getfreeslots)]
    (if-let [choosen (get free slot)]
      (let [updfree (disj free choosen)
            updbooked (conj booked (->Booking choosen name))]
        (->BookingSystem updfree updbooked))
      (throw (Exception. "No such slot exists")))))

(defn book-apt [start end name]
  (if (not (empty? name))
    (dosync
      (alter appts with-new start end name))
    (throw (Exception. "Name was empty"))))

(defn elempara [text]
  (x/element :p {} text))

(defn htmldoc [title & content]
  (x/element :html {}
    (x/element :head {}
      (x/element :title {} title))
    (x/element :body {}
      (x/element :h1 {} title)
      content)))

(defn elem-table [header rows]
  "Takes header (list of strings) and rows (list of arrays of string elements)
and outputs a html table element using these."
  (x/element :table {}
    (x/element :tr {}
      (map #(x/element :th {} %) header))
    (map getrow rows)))

(defroutes app
  (GET "/" []
    (x/emit-str
      (htmldoc "Appointment Booking System"
        (elempara
          (x/element :a {:href "/slots"} "View available free slots"))
        (elempara
          (x/element :a {:href "/bookings"} "View all existing bookings")))))
  (GET "/slots" []
    (x/emit-str
      (htmldoc "Appointment Free Slots"
        (elem-table ["Begin" "End" "Name"] (getfreeslots))
        (elempara
          (x/element :a {:href "/bookings"} "View all existing bookings")))))
  (GET "/bookings" []
    (x/emit-str
      (htmldoc "Booked Appointments"
        (elem-table ["Start" "End" "Name"] (getbookings))
        (elempara
          (x/element :a {:href "/slots"} "View free slots to book")))))
  (POST "/bookapt" [begin end name]
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
              (x/element :a {:href "/slots"} "Try booking another free slot"))))))))

(def handler
  (-> app wrap-params))
