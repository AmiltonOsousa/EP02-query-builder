
(ns consultas.compilador)

(defn- comparar
  "Aplica teste ao compare entre o campo do registro e o literal.
  Serve para numeros (inteiro com decimal) e para textos."
  [teste campo valor registro]
  (teste (compare (get registro campo) valor)))

(defn compilar-expressao
  [expr]
  (case (first expr)

    :=
    (let [[_ campo valor] expr]
      (fn [registro]
        (let [valor-registro (get registro campo)]
          (if (and (number? valor-registro)
                   (number? valor))
            (== valor-registro valor)
            (= valor-registro valor)))))

    :!=
    (let [[_ campo valor] expr]
      (fn [registro]
        (let [valor-registro (get registro campo)]
          (if (and (number? valor-registro)
                   (number? valor))
            (not (== valor-registro valor))
            (not= valor-registro valor)))))

    :<
    (let [[_ campo valor] expr]
      (partial comparar neg? campo valor))

    :<=
    (let [[_ campo valor] expr]
      (partial comparar (complement pos?) campo valor))

    :>
    (let [[_ campo valor] expr]
      (partial comparar pos? campo valor))

    :>=
    (let [[_ campo valor] expr]
      (partial comparar (complement neg?) campo valor))

    :e
    (let [[_ esquerda direita] expr
          avaliar-esquerda (compilar-expressao esquerda)
          avaliar-direita (compilar-expressao direita)]
      (fn [registro]
        (and (avaliar-esquerda registro)
             (avaliar-direita registro))))

    :ou
    (let [[_ esquerda direita] expr
          avaliar-esquerda (compilar-expressao esquerda)
          avaliar-direita (compilar-expressao direita)]
      (fn [registro]
        (or (avaliar-esquerda registro)
            (avaliar-direita registro))))

    :nao
    (let [[_ interna] expr
          avaliar-interna (compilar-expressao interna)]
      (fn [registro]
        (not (avaliar-interna registro))))))

(defn compilar-estagio
  [estagio]
  (case (first estagio)
    :onde
    (let [[_ expr] estagio]
      (partial filter (compilar-expressao expr)))

    :ordenar-por
    (let [[_ campo ordem] estagio
          comparador (if (= ordem :desc)
                       (fn [a b] (compare b a))
                       compare)]
      (partial sort-by campo comparador))

    :limitar
    (let [[_ n] estagio]
      (partial take n))))

    
(defn compilar-terminal
  [terminal]
  (case (first terminal)
    :selecionar
    (let [[_ campos] terminal]
      (partial map #(select-keys % campos)))

    :contar
    (partial reduce (fn [n _] (inc n)) 0)

    :soma
    (let [[_ campo] terminal]
      (partial reduce (fn [total registro] (+ total (get registro campo))) 0))

    :media
    (let [[_ campo] terminal
          somar (compilar-terminal [:soma campo])
          contar (compilar-terminal [:contar])]
      (fn [registros]
        (let [n (contar registros)]
          (if (zero? n)
            0.0
            (double (/ (somar registros) n))))))

    :agrupar-por
    (let [[_ campo agregacao] terminal
          executar-agregacao (compilar-terminal agregacao)]
      (fn [registros]
        (into {}
              (map (fn [[chave grupo]]
                     [chave (executar-agregacao grupo)])
                   (group-by campo registros)))))))

