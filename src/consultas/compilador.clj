
(ns consultas.compilador)

(defn compilar-expressao
  [expr]
  (case (first expr)
    :=
    (let [[_ campo valor] expr]
      (fn [registro]
        (= (get registro campo) valor)))

    :!=
    (let [[_ campo valor] expr]
      (fn [registro]
        (not= (get registro campo) valor)))

    :<
    (let [[_ campo valor] expr]
      (fn [registro]
        (< (get registro campo) valor)))

    :<=
    (let [[_ campo valor] expr]
      (fn [registro]
        (<= (get registro campo) valor)))

    :>
    (let [[_ campo valor] expr]
      (fn [registro]
        (> (get registro campo) valor)))

    :>=
    (let [[_ campo valor] expr]
      (fn [registro]
        (>= (get registro campo) valor)))

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
    (let [[_ expr] estagio
          avaliar (compilar-expressao expr)]
      (fn [registros]
        (filter avaliar registros)))

    :ordenar-por
    (let [[_ campo ordem] estagio
          comparador (if (= ordem :desc)
                       (fn [a b] (compare b a))
                       compare)]
      (fn [registros]
        (sort-by campo comparador registros)))

    :limitar
    (let [[_ n] estagio]
      (fn [registros]
        (take n registros)))))

    
(defn compilar-terminal
  [terminal]
  (case (first terminal)
    :selecionar
    (let [[_ campos] terminal]
      (fn [registros]
        (map (fn [registro]
               (select-keys registro campos))
             registros)))

    :contar
    (fn [registros]
      (count registros))

    :soma
    (let [[_ campo] terminal]
      (fn [registros]
        (reduce + 0 (map campo registros))))

    :media
    (let [[_ campo] terminal]
      (fn [registros]
        (let [valores (map campo registros)]
          (if (seq valores)
            (double (/ (reduce + 0 valores)
                       (count valores)))
            0.0))))

    :agrupar-por
    (let [[_ campo agregacao] terminal
          executar-agregacao (compilar-terminal agregacao)]
      (fn [registros]
        (into {}
              (map (fn [[chave grupo]]
                     [chave (executar-agregacao grupo)])
                   (group-by campo registros)))))))

