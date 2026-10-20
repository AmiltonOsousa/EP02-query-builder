
(ns testar-casos
  (:require [clojure.test :refer [deftest is run-tests testing]]
            [clojure.string :as str]
            [consultas.core :as core]
            [consultas.csv :as csv]))

(def dados (csv/ler-csv "dados/filmes.csv"))

(defn executar-seguro
  [consulta]
  (try
    (let [resultado (core/executar consulta dados)]
      (if (and (map? resultado) (contains? resultado :erros))
        (str/join " " (:erros resultado))
        resultado))
    (catch Exception e
      (.getMessage e))))

(defn resultado-erro?
  [resultado]
  (and (string? resultado)
       (or (str/includes? (str/upper-case resultado) "ERRO")
           (str/includes? (str/lower-case resultado) "campo")
           (str/includes? (str/lower-case resultado) "sintaxe")
           (str/includes? (str/lower-case resultado) "terminal")
           (str/includes? (str/lower-case resultado) "operador")
           (str/includes? (str/lower-case resultado) "literal")
           (str/includes? (str/lower-case resultado) "estagio")
           (str/includes? (str/lower-case resultado) "elemento")
           (str/includes? (str/lower-case resultado) "parentes")
           (str/includes? (str/lower-case resultado) "limite")
           (str/includes? (str/lower-case resultado) "ordenar")
           (str/includes? (str/lower-case resultado) "soma")
           (str/includes? (str/lower-case resultado) "numero"))))

(defn titulos
  [consulta]
  (mapv :titulo (executar-seguro consulta)))

(deftest projecao-e-corte
  (testing "Consulta vazia retorna todos os registros"
    (let [resultado (executar-seguro "")]
      (is (= 8 (count resultado)))
      (is (= "Cidade de Deus" (:titulo (first resultado))))
      (is (= "Ainda Estou Aqui" (:titulo (last resultado))))))
  (testing "Contagem"
    (is (= 8 (executar-seguro "contar"))))
  (testing "Limitar e selecionar"
    (is (= ["Cidade de Deus"
            "Central do Brasil"
            "O Auto da Compadecida"]
           (titulos "limitar 3 | selecionar titulo"))))
  (testing "Filtrar sem selecionar mantém todos os campos"
    (let [resultado (first (executar-seguro
                            "onde titulo = \"Aquarius\""))]
      (is (= {:titulo "Aquarius"
              :ano 2016
              :genero "drama"
              :nota 7.5
              :duracao 145}
             resultado))))
  (testing "Seleção respeita os campos solicitados"
    (is (= [{:ano 2002 :titulo "Cidade de Deus"}]
           (vec (executar-seguro
                 "onde titulo = \"Cidade de Deus\" | selecionar ano, titulo"))))))

(deftest ordenacao
  (testing "Ordenação crescente estável"
    (is (= [{:titulo "Central do Brasil" :ano 1998}
            {:titulo "O Auto da Compadecida" :ano 2000}
            {:titulo "Cidade de Deus" :ano 2002}]
           (vec (executar-seguro
                 "ordenar por ano | limitar 3 | selecionar titulo, ano")))))
  (testing "Ordenação decrescente mantém a ordem entre empates"
    (is (= ["Cidade de Deus"
            "O Auto da Compadecida"
            "Ainda Estou Aqui"
            "Central do Brasil"
            "Tropa de Elite"
            "Que Horas Ela Volta"
            "Aquarius"
            "Bacurau"]
           (titulos "ordenar por nota desc | selecionar titulo"))))
  (testing "Ordenação crescente mantém a ordem entre empates"
    (is (= ["Bacurau"
            "Aquarius"
            "Que Horas Ela Volta"
            "Central do Brasil"
            "Tropa de Elite"
            "Ainda Estou Aqui"
            "Cidade de Deus"
            "O Auto da Compadecida"]
           (titulos "ordenar por nota | selecionar titulo")))))

(deftest expressoes
  (testing "Parênteses e negação"
    (is (= ["Bacurau"]
           (titulos
            "onde (genero = \"acao\" ou genero = \"suspense\") e nao ano < 2010 | selecionar titulo"))))
  (testing "E tem precedência sobre ou"
    (is (= ["Tropa de Elite"]
           (titulos
            "onde genero = \"acao\" ou genero = \"suspense\" e ano < 2010 | selecionar titulo"))))
  (testing "Inteiro e decimal são comparados numericamente"
    (is (= ["Central do Brasil" "Tropa de Elite"]
           (titulos "onde nota = 8 | selecionar titulo"))))
  (testing "Desigualdade numérica"
    (is (= 6 (executar-seguro "onde nota != 8.6 | contar"))))
  (testing "Negação de comparação"
    (is (= ["Bacurau" "Que Horas Ela Volta" "Aquarius"]
           (titulos "onde nao nota >= 8 | selecionar titulo")))))

(deftest terminais
  (testing "Soma de inteiro"
    (is (= 987 (executar-seguro "soma duracao"))))
  (testing "Soma de decimal"
    (is (= 64.0 (executar-seguro "soma nota"))))
  (testing "Média"
    (is (= 8.0 (executar-seguro "media nota"))))
  (testing "Agrupamento com média"
    (is (= {"acao" 115.0
            "comedia" 104.0
            "drama" 127.4
            "suspense" 131.0}
           (executar-seguro "agrupar por genero com media duracao"))))
  (testing "Agrupamento com soma"
    (is (= {"acao" 115
            "comedia" 104
            "drama" 637
            "suspense" 131}
           (executar-seguro "agrupar por genero com soma duracao"))))
  (testing "Agrupamento por campo decimal"
    (is (= {7.4 1, 7.5 1, 7.7 1, 8.0 2, 8.2 1, 8.6 2}
           (executar-seguro "agrupar por nota com contar"))))
  (testing "Agrupamento após filtro"
    (is (= {"drama" 3, "suspense" 1}
           (executar-seguro
            "onde ano >= 2010 | agrupar por genero com contar")))))

(deftest resultados-vazios
  (testing "Contagem sem registros"
    (is (= 0 (executar-seguro "onde ano > 3000 | contar"))))
  (testing "Soma sem registros"
    (is (= 0 (executar-seguro "onde ano > 3000 | soma duracao"))))
  (testing "Média sem registros"
    (is (= 0.0 (executar-seguro "onde ano > 3000 | media nota"))))
  (testing "Consulta sem registros"
    (is (= [] (vec (executar-seguro "onde ano > 3000")))))
  (testing "Agrupamento sem registros"
    (is (= {} (executar-seguro
               "onde ano > 3000 | agrupar por genero com contar")))))

(deftest erros-de-esquema
  (testing "Campos inexistentes são rejeitados"
    (let [resultado (executar-seguro
                     "onde diretor = \"x\" ou estudio = \"y\"")]
      (is (string? resultado))
      (is (str/includes? resultado "diretor"))
      (is (str/includes? resultado "estudio"))))
  (testing "Seleção de campos inexistentes"
    (let [resultado (executar-seguro "selecionar titulo, pais, idioma")]
      (is (string? resultado))
      (is (or (str/includes? resultado "pais")
              (str/includes? resultado "idioma")))))
  (testing "Consulta com múltiplos problemas é recusada"
    (is (string? (executar-seguro
                  "onde ano = \"x\" e pais = \"BR\" | soma titulo"))))
  (testing "Ordenação por campo inexistente"
    (let [resultado (executar-seguro "ordenar por diretor")]
      (is (string? resultado))
      (is (str/includes? resultado "diretor"))))
  (testing "Soma de campo textual"
    (let [resultado (executar-seguro "soma titulo")]
      (is (string? resultado))
      (is (str/includes? resultado "titulo")))))

(deftest erros-de-sintaxe
  (run! (fn [consulta]
          (testing (str "Consulta inválida: " consulta)
            (is (string? (executar-seguro consulta))
                (str "A consulta deveria ser recusada: " consulta))))
        ["|"
         "voar"
         "onde titulo contem \"de\""
         "pular 2"
         "limitar abc"
         "contar | limitar 2"
         "selecionar titulo | onde ano > 2000"
         "selecionar titulo | contar"
         "minimo ano"
         "onde (ano > 2000"
         "ordenar ano"
         "agrupar por genero"
         "onde ano >"]))

(deftest campos-exigidos-nos-erros
  (testing "Erro de campo inexistente cita o campo"
    (let [resultado (executar-seguro "ordenar por diretor")]
      (is (string? resultado))
      (is (str/includes? resultado "diretor"))))
  (testing "Erro de soma cita o campo textual"
    (let [resultado (executar-seguro "soma titulo")]
      (is (string? resultado))
      (is (str/includes? resultado "titulo")))))

(let [resultado (run-tests 'testar-casos)]
  (when (pos? (+ (:fail resultado) (:error resultado)))
    (System/exit 1)))
