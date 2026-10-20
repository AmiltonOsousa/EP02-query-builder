
(ns consultas.main
  (:require [clojure.string :as str]
            [consultas.core :as core]
            [consultas.csv :as csv]
            [consultas.sintaxe :as sintaxe]))

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

(defn- campos-de-saida
  "Campos na ordem de saida: a do selecionar ou, sem ele, a do esquema."
  [ast esquema]
  (let [[tipo campos] (:terminal ast)]
    (if (= tipo :selecionar)
      campos
      (map first esquema))))

(defn- imprimir-resultado
  [resultado campos]
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
    (doseq [registro resultado]
      (imprimir-registro registro campos))

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
              (= entrada "EXIT")
              nil

              (str/blank? entrada)
              (do
                (println "ERRO: linha em branco.")
                (recur))

              (or (= entrada "QUERY")
                  (str/starts-with? entrada "QUERY "))
              (do
                (try
                  (let [ast (sintaxe/analisar (subs entrada 5))
                        resultado (core/executar ast dados)]
                    (imprimir-resultado
                     resultado
                     (campos-de-saida ast (:esquema dados))))
                  (catch Exception e
                    (println (str "ERRO: " (.getMessage e)))))
                (recur))

              :else
              (do
                (println "ERRO: comando invalido. Use QUERY ou EXIT.")
                (recur)))))))
    (catch Exception e
      (println (str "ERRO: " (.getMessage e))))))
