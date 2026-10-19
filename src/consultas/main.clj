
(ns consultas.main
  (:require [clojure.string :as str]
            [consultas.core :as core]
            [consultas.csv :as csv]))

(defn- formatar-valor
  [valor]
  (if (and (number? valor)
           (not (integer? valor)))
    (String/format java.util.Locale/US "%.2f"
                   (to-array [(double valor)]))
    (str valor)))

(defn- imprimir-resultado
  [resultado]
  (cond
    (and (map? resultado) (contains? resultado :erros))
    (doseq [erro (:erros resultado)]
      (println (str "ERRO: " erro)))

    (number? resultado)
    (println (formatar-valor resultado))

    (map? resultado)
    (if (empty? resultado)
      (println "(vazio)")
      (doseq [[chave valor] (sort-by key resultado)]
        (println (str (name chave) ": " (formatar-valor valor)))))

    (seq resultado)
    (doseq [registro resultado]
      (println
       (str/join
        " | "
        (map (fn [[campo valor]]
               (str (name campo) ": " (formatar-valor valor)))
             registro))))

    :else
    (println "(vazio)")))


(defn -main
  [& args]
  (try
    (let [caminho (or (first args) "dados/filmes.csv")
          dados (csv/ler-csv caminho)]
      (loop []
        (print "> ")
        (flush)
        (when-let [linha (read-line)]
          (let [entrada (str/trim linha)]
            (cond
              (= (str/upper-case entrada) "EXIT")
              nil

              (str/blank? entrada)
              (do
                (println "ERRO: consulta vazia.")
                (recur))

              (str/starts-with? (str/upper-case entrada) "QUERY ")
              (do
                (try
                  (let [consulta (subs entrada 6)
                        resultado (core/executar consulta dados)]
                    (imprimir-resultado resultado))
                  (catch Exception e
                    (println (str "ERRO: " (.getMessage e)))))
                (recur))

              :else
              (do
                (println "ERRO: comando invalido. Use QUERY ou EXIT.")
                (recur)))))))
    (catch Exception e
      (println (str "ERRO: " (.getMessage e))))))
