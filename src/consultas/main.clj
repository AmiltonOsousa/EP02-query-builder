
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

(defn- imprimir-registro
  [registro campos]
  (println
   (str/join "; "
             (map #(formatar-valor (get registro %))
                  campos))))

(defn- imprimir-resultado
  [resultado esquema]
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
        (println (str (formatar-valor chave)
                      "; "
                      (formatar-valor valor)))))

    (seq resultado)
    (let [campos (if (seq resultado)
                   (keys (first resultado))
                   (map first esquema))]
      (doseq [registro resultado]
        (imprimir-registro registro campos)))

    :else
    (println "(vazio)")))

(defn -main
  [& args]
  (try
    (let [caminho (or (first args) "dados/filmes.csv")
          dados (csv/ler-csv caminho)
          campos (map first (:esquema dados))]
      (loop []
        (print "> ")
        (flush)
        (when-let [linha (read-line)]
          (let [entrada (str/trim linha)]
            (cond
              (= entrada "EXIT")
              nil

              (str/blank? entrada)
              (do
                (println "ERRO: linha em branco.")
                (recur))

              (= entrada "QUERY")
              (do
                (imprimir-resultado (:registros dados) (:esquema dados))
                (recur))

              (str/starts-with? entrada "QUERY ")
              (do
                (try
                  (let [consulta (str/trim (subs entrada 6))
                        resultado (core/executar consulta dados)]
                    (if (and (map? resultado)
                             (contains? resultado :erros))
                      (imprimir-resultado resultado (:esquema dados))
                      (if (seq (:terminal consulta))
                        (imprimir-resultado resultado (:esquema dados))
                        (imprimir-resultado resultado (:esquema dados)))))
                  (catch Exception e
                    (println (str "ERRO: " (.getMessage e)))))
                (recur))

              :else
              (do
                (println "ERRO: comando invalido. Use QUERY ou EXIT.")
                (recur)))))))
    (catch Exception e
      (println (str "ERRO: " (.getMessage e))))))
