VRP SA Code
===========

This is all code which I developed for my PhD thesis; it has a few major parts

 * A useful timing library in h.util.timing
 * An option parsing library in h.options
 * An s-expression parser in h.sexp
 * A VRP solver in h.vrp, including
   * A simulated annealing solver, which can perform n-opt-k moves, and osman moves
   * A parser for the standard instance format used in the literature
   * A neighbourhood list implementation
 * A stochastic Clarke-Wright implementation in h.vrp.stochasticsavings
 * A traditional and enhanced CW implementation in h.solcons
 * A GUI which can show any of these solvers running interactively