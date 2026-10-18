
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
    (let [[_ campo ordem] estagio]
      (fn [registros]
        (sort-by campo
                 (if (= ordem :desc) > <)
                 registros)))

    :limitar
    (let [[_ n] estagio]
      (fn [registros]
        (take n registros)))))
