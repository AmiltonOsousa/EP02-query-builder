
(ns consultas.csv
  (:require [clojure.string :as str]))

(defn- converter-tipo
  [valor tipo]
  (case tipo
    "texto" valor
    "inteiro" (Long/parseLong valor)
    "decimal" (Double/parseDouble valor)))

(defn- analisar-coluna
  [coluna]
  (let [[campo tipo] (str/split coluna #":" 2)]
    (when (or (str/blank? campo)
              (not (contains? #{"texto" "inteiro" "decimal"} tipo)))
      (throw (ex-info
              (str "Definicao de coluna invalida: " coluna)
              {:coluna coluna})))
    [(keyword campo) (keyword tipo)]))

(defn- analisar-cabecalho
  [linha]
  (let [colunas (map analisar-coluna (str/split linha #";"))]
    (when (empty? colunas)
      (throw (ex-info "CSV sem colunas." {})))
    (when-not (= (count colunas) (count (distinct (map first colunas))))
      (throw (ex-info "Existem colunas duplicadas no CSV." {})))
    (vec colunas)))

(defn- analisar-registro
  [linha esquema]
  (let [valores (str/split linha #";" -1)]
    (when-not (= (count valores) (count esquema))
      (throw (ex-info
              (str "Quantidade de valores incorreta na linha: " linha)
              {:linha linha})))
    (into {}
          (map (fn [[[campo tipo] valor]]
                 [campo (converter-tipo valor (name tipo))])
               (map vector esquema valores)))))

(defn ler-csv
  [caminho]
  (let [linhas (str/split-lines (slurp caminho))]
    (when (empty? linhas)
      (throw (ex-info "O arquivo CSV esta vazio." {})))
    (let [esquema (analisar-cabecalho (first linhas))
          registros (mapv #(analisar-registro % esquema)
                          (rest linhas))]
      {:esquema esquema
       :registros registros})))
