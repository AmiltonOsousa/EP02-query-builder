
(ns consultas.core
  (:require [consultas.compilador :as compilador]
            [consultas.sintaxe :as sintaxe]))

(defn consulta
  []
  {:estagios []
   :terminal nil})

(defn- adicionar-estagio
  [c estagio]
  (if (:terminal c)
    (throw (ex-info "Nao e permitido adicionar estagios depois do terminal."
                    {:terminal (:terminal c)}))
    (update c :estagios conj estagio)))

(defn onde
  [c expr]
  (adicionar-estagio c [:onde expr]))

(defn selecionar
  [c campos]
  (if (:terminal c)
    (throw (ex-info "A consulta ja possui um terminal."
                    {:terminal (:terminal c)}))
    (assoc c :terminal [:selecionar campos])))

(defn ordenar-por
  [c campo ordem]
  (adicionar-estagio c [:ordenar-por campo ordem]))

(defn limitar
  [c n]
  (adicionar-estagio c [:limitar n]))

(defn contar
  [c]
  (if (:terminal c)
    (throw (ex-info "A consulta ja possui um terminal."
                    {:terminal (:terminal c)}))
    (assoc c :terminal [:contar])))

(defn soma
  [c campo]
  (if (:terminal c)
    (throw (ex-info "A consulta ja possui um terminal."
                    {:terminal (:terminal c)}))
    (assoc c :terminal [:soma campo])))

(defn media
  [c campo]
  (if (:terminal c)
    (throw (ex-info "A consulta ja possui um terminal."
                    {:terminal (:terminal c)}))
    (assoc c :terminal [:media campo])))

(defn agrupar-por
  [c campo agregacao]
  (if (:terminal c)
    (throw (ex-info "A consulta ja possui um terminal."
                    {:terminal (:terminal c)}))
    (assoc c :terminal [:agrupar-por campo agregacao])))

(defn- normalizar-campo
  [campo]
  (if (keyword? campo)
    campo
    (keyword campo)))

(defn- mapa-esquema
  [dados]
  (into {}
        (map (fn [[campo tipo]]
               [(normalizar-campo campo) tipo])
             (:esquema dados))))

(defn- tipo-numerico?
  [tipo]
  (contains? #{:inteiro :decimal} tipo))

(defn- tipo-literal
  [valor]
  (cond
    (string? valor) :texto
    (integer? valor) :inteiro
    (number? valor) :decimal
    :else nil))

(defn- validar-expressao
  [expr esquema]
  (case (first expr)
    (:e :ou)
    (let [[_ esquerda direita] expr]
      (into (validar-expressao esquerda esquema)
            (validar-expressao direita esquema)))

    :nao
    (validar-expressao (second expr) esquema)

    (:= :!= :< :<= :> :>=)
    (let [[_ campo valor] expr
          tipo-campo (get esquema campo)
          tipo-valor (tipo-literal valor)]
      (cond
        (nil? tipo-campo)
        [(str "campo inexistente: " (name campo))]

        (nil? tipo-valor)
        [(str "Literal invalido no campo " (name campo))]

        (or (and (tipo-numerico? tipo-campo)
                 (tipo-numerico? tipo-valor))
            (= tipo-campo tipo-valor))
        []

        :else
        [(str "comparacao entre "
              (name tipo-campo) " e "
              (name tipo-valor) " no campo "
              (name campo))]))))


(defn- validar-campo
  [campo esquema]
  (if (contains? esquema campo)
    []
    [(str "campo inexistente: " (name campo))]))

(defn- validar-agregacao
  [agregacao esquema]
  (case (first agregacao)
    :contar
    []

    (:soma :media)
    (let [campo (second agregacao)
          tipo (get esquema campo)]
      (cond
        (nil? tipo)
        (validar-campo campo esquema)

        (tipo-numerico? tipo)
        []

        :else
        [(str (name (first agregacao))
              " exige campo numerico: "
              (name campo))]))))


(defn- validar-terminal
  [terminal esquema]
  (case (first terminal)
    :selecionar
    (reduce (fn [erros campo]
              (into erros (validar-campo campo esquema)))
            []
            (second terminal))

    :contar
    []

    (:soma :media)
    (validar-agregacao terminal esquema)

    :agrupar-por
    (let [[_ campo agregacao] terminal]
      (into (validar-campo campo esquema)
            (validar-agregacao agregacao esquema)))))

(defn- validar-consulta
  [c esquema]
  (let [erros-estagios
        (reduce
         (fn [erros estagio]
           (into erros
                 (case (first estagio)
                   :onde
                   (validar-expressao (second estagio) esquema)

                   :ordenar-por
                   (validar-campo (second estagio) esquema)

                   :limitar
                   [])))
         []
         (:estagios c))]
    (into erros-estagios
          (if (:terminal c)
            (validar-terminal (:terminal c) esquema)
            []))))

(defn executar
  [c dados]
  (let [c (if (string? c)
            (sintaxe/analisar c)
            c)
        esquema (mapa-esquema dados)
        erros (validar-consulta c esquema)]
    (if (seq erros)
      {:erros erros}
      (let [estagios-compilados
            (map compilador/compilar-estagio
                 (:estagios c))
            pipeline
            (reduce (fn [funcao estagio]
                      (comp estagio funcao))
                    identity
                    estagios-compilados)
            resultado (pipeline (:registros dados))]
        (if-let [terminal (:terminal c)]
          ((compilador/compilar-terminal terminal) resultado)
          resultado)))))
