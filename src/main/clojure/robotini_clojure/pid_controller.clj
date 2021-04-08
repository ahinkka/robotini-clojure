(ns robotini-clojure.pid-controller)

;; This is a port of Bempu's Python PID, see
;; https://github.com/reaktor/codecamp-2021-robotini-car-template/pull/1/files

(defn pid-controller
  "
  A simple PID controller for smooth operations.
  (https://en.wikipedia.org/wiki/PID_controller)
  PID is a simple controller that adjusts the controller output
  (e.g. steering angle of the car) based on the input (e.g. desired steering
  angle) given to the controller.

  PID stands for proportional, integral and derivative.  These three terms
  affect the output value based on the given Kp, Ki and Kd constants. The Kp
  value adjusts the weight of the proportional term, the Ki value adjusts the
  weight of the integral term and Kd the derivative term.  Every iteration,
  PID calculates an error based on the input and setpoint parameters, as well
  as its internal state:

    - The proportional term calculates a steering value directly
      proportional to the error this very same iteration. This term
      makes the controller output act fast, but will start to
      oscillate at higher values.

    - The integral term maintains a cumulative error, summing
      up the past error values from previous iterations, sort of
      acting as a memory, for the controller. This term adds
      slowness to the controller output, but removes the steady-state
      error (e.g. the car never converges exactly to the middle of the lane).

    - The derivative term calculates a rate of change (delta) for
      the error. The bigger the rate of error change between iterations
      is, the more weight the d-term has. In some cases, the D-term weight
      can be increased to counter-balance oscillations caused by a large
      p-term.

  There are sophisticated ways of tuning these parameters, but if you don't
  know what you are doing, the good old Harrison-Stetson method (i.e. just guess)
  will probably suffice. The parameters should be tuned based on the car's behavior.
  Harrison-Stetson: you might want to start with setting the P term to some value
  (e.g. 1.0), and leaving I and D at 0. Increase if the steering is too slow,
  decrease if too fast or if there's too much oscillation. After that, start
  adding D term, until it counter-balances the oscillation.  Add a flavor of
  i-term if needed. It's probably not needed. In that case, you might want to
  study integral windup and adjust that parameter, too.

  Usage, e.g.
    ; Initialize PID
    (loop [pid (pid-controller kp kd ki anti-windup)] ; here kp, kd, ki are parameters
                                                      ; to tune, anti-windup the max/min
                                                      ; reasonable value for the variable
      <code that calculates the value to be controlled, e.g. measured angle>
      (let [[pid pid-controlled-value]
            (step pid desired-value measured-value)] ; here desired-value is e.g. where
                                                     ; you want to point, measured-value
                                                     ; is the one you are at right now
        <code that uses the controlled value>
        (recur pid)))
  "
  [Kp Ki Kd anti-windup]
  {:pre [(number? Kp)
         (number? Ki)
         (number? Kd)]}
   {:Kp Kp :Ki Ki :Kd Kd
    :anti-windup anti-windup
    :error 0.0 :prev-error 0.0 :rate-error 0.0 :cumulative-error 0.0})

(defn clamp [value min-val max-val] (max min-val (min value max-val)))

(defn step
  [pid setpoint value]
  {:pre [(number? setpoint)
         (number? value)]}
  (let [new-error (- setpoint value)
        new-cumulative-error (clamp (+ (:cumulative-error pid) new-error) (- (:anti-windup pid)) (:anti-windup pid))
        new-rate-error (- new-error (:prev-error pid))
        p-term (* (:Kp pid) new-error)
        i-term (* (:Ki pid) new-cumulative-error)
        d-term (* (:Kd pid) new-rate-error)]
    [(merge
      pid
      {:error new-error :cumulative-error new-cumulative-error :rate-error new-rate-error :prev-error new-error})
     (+ p-term i-term d-term)]))
