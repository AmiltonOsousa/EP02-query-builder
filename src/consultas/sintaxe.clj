
(ns consultas.sintaxe
  (:require [clojure.string :as str]))

(defn- tokenizar
  [texto]
  (re-seq #"\"[^\"]*\"|>=|<=|!=|[(),|=<>]|-?\d+(?:\.\d+)?|[^\s(),|=<>!]+"
          texto))

(defn- erro-sintaxe
  [mensagem tokens]
  (throw (ex-info mensagem {:tokens-restantes tokens})))

(declare analisar-ou)

(defn- analisar-campo
  [token]
  (when (or (nil? token)
            (contains? #{"e" "ou" "nao" "com" "desc"}
                       token))
    (erro-sintaxe "Campo ausente ou invalido." [token]))
  (keyword token))

(defn- analisar-literal
  [token]
  (cond
    (nil? token)
    (erro-sintaxe "Literal ausente." [])

    (and (str/starts-with? token "\"")
         (str/ends-with? token "\""))
    (subs token 1 (dec (count token)))

    (re-matches #"-?\d+" token)
    (parse-long token)

    (re-matches #"-?\d+\.\d+" token)
    (parse-double token)

    :else
    (erro-sintaxe "Literal invalido." [token])))

(defn- analisar-primaria
  [tokens]
  (if (= "(" (first tokens))
    (let [[expr restantes] (analisar-ou (rest tokens))]
      (when-not (= ")" (first restantes))
        (erro-sintaxe "Faltou fechar parenteses." restantes))
      [expr (rest restantes)])
    (let [[campo operador literal & restantes] tokens
          campo (analisar-campo campo)
          operador (case operador
                     "=" :=
                     "!=" :!=
                     "<" :<
                     "<=" :<=
                     ">" :>
                     ">=" :>=
                     (erro-sintaxe
                      "Operador de comparacao invalido."
                      [operador]))
          valor (analisar-literal literal)]
      [[operador campo valor] restantes])))

(defn- analisar-nao
  [tokens]
  (if (= "nao" (first tokens))
    (let [[expr restantes] (analisar-nao (rest tokens))]
      [[:nao expr] restantes])
    (analisar-primaria tokens)))

(defn- analisar-e
  [tokens]
  (let [[esquerda restantes] (analisar-nao tokens)]
    (if (= "e" (first restantes))
      (let [[direita finais] (analisar-e (rest restantes))]
        [[:e esquerda direita] finais])
      [esquerda restantes])))

(defn- analisar-ou
  [tokens]
  (let [[esquerda restantes] (analisar-e tokens)]
    (if (= "ou" (first restantes))
      (let [[direita finais] (analisar-ou (rest restantes))]
        [[:ou esquerda direita] finais])
      [esquerda restantes])))

(defn- analisar-expressao
  [tokens]
  (let [[expr restantes] (analisar-ou tokens)]
    (when (seq restantes)
      (erro-sintaxe "Tokens inesperados na expressao." restantes))
    expr))

(defn- analisar-campos
  [tokens]
  (let [[campo & restantes] tokens]
    (loop [campos [(analisar-campo campo)]
           tokens restantes]
      (if (= "," (first tokens))
        (recur (conj campos
                     (analisar-campo (second tokens)))
               (nnext tokens))
        (do
          (when (seq tokens)
            (erro-sintaxe "Lista de campos invalida." tokens))
          campos)))))

(defn- analisar-agregacao
  [tokens]
  (case (first tokens)
    "contar"
    (if (next tokens)
      (erro-sintaxe "Tokens inesperados apos contar."
                    (next tokens))
      [:contar])

    "soma"
    (if (= 2 (count tokens))
      [:soma (analisar-campo (second tokens))]
      (erro-sintaxe "Agregacao soma invalida." tokens))

    "media"
    (if (= 2 (count tokens))
      [:media (analisar-campo (second tokens))]
      (erro-sintaxe "Agregacao media invalida." tokens))

    (erro-sintaxe "Agregacao desconhecida." tokens)))

(defn- separar-elementos
  [tokens]
  (reduce (fn [grupos token]
            (if (= "|" token)
              (conj grupos [])
              (update grupos (dec (count grupos)) conj token)))
          [[]]
          tokens))

(defn- analisar-elemento
  [tokens]
  (let [tokens (vec tokens)]
    (when (empty? tokens)
      (erro-sintaxe "Elemento vazio." tokens))
    (case (first tokens)
      "onde"
      [:estagio
       [:onde (analisar-expressao (subvec tokens 1))]]

      "ordenar"
      (let [[_ por campo & restantes] tokens]
        (when-not (= "por" por)
          (erro-sintaxe "Esperado 'ordenar por'." tokens))
        (when-not (or (empty? restantes)
                      (= ["desc"] restantes))
          (erro-sintaxe "Ordem de classificacao invalida."
                        restantes))
        [:estagio
         [:ordenar-por
          (analisar-campo campo)
          (if (= ["desc"] restantes) :desc :asc)]])

      "limitar"
      (if (and (= 2 (count tokens))
               (re-matches #"\d+" (second tokens)))
        [:estagio [:limitar (parse-long (second tokens))]]
        (erro-sintaxe "Limite invalido." tokens))

      "selecionar"
      [:terminal
       [:selecionar (analisar-campos (subvec tokens 1))]]

      "contar"
      (if (= 1 (count tokens))
        [:terminal [:contar]]
        (erro-sintaxe "Tokens inesperados apos contar."
                      tokens))

      "soma"
      (if (= 2 (count tokens))
        [:terminal [:soma (analisar-campo (second tokens))]]
        (erro-sintaxe "Terminal soma invalido." tokens))

      "media"
      (if (= 2 (count tokens))
        [:terminal [:media (analisar-campo (second tokens))]]
        (erro-sintaxe "Terminal media invalido." tokens))

      "agrupar"
      (let [[_ por campo com & agregacao] tokens]
        (when-not (and (= "por" por) (= "com" com))
          (erro-sintaxe
           "Sintaxe esperada: agrupar por campo com agregacao."
           tokens))
        [:terminal
         [:agrupar-por
          (analisar-campo campo)
          (analisar-agregacao agregacao)]])

      (erro-sintaxe "Elemento desconhecido." tokens))))

(defn analisar
  [texto]
  (let [tokens (tokenizar texto)]
    (if (empty? tokens)
      {:estagios [] :terminal nil}
      (let [partes (separar-elementos tokens)
            elementos (mapv analisar-elemento partes)
            terminais (keep-indexed
                       (fn [i elemento]
                         (when (= :terminal (first elemento)) i))
                       elementos)]
        (when (and (seq terminais)
                   (or (> (count terminais) 1)
                       (not= (first terminais)
                             (dec (count elementos)))))
          (erro-sintaxe
           "O terminal deve ser o ultimo elemento."
           partes))
        {:estagios
         (mapv second
               (filter #(= :estagio (first %)) elementos))
         :terminal
         (let [ultimo (last elementos)]
           (when (= :terminal (first ultimo))
             (second ultimo)))}))))
