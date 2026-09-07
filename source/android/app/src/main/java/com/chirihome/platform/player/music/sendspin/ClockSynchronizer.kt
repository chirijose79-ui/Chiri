package com.chirihome.platform.player.music.sendspin

import kotlin.time.TimeSource

/**
 * Sincroniza el reloj local de Chiri con el reloj del servidor Sendspin.
 *
 * El reproductor Sendspin necesita convertir timestamps del servidor
 * a timestamps del reloj monotónico local para poder reproducir el
 * audio en el instante correcto.
 *
 * Fase 1:
 * - Reloj monotónico local.
 * - Cálculo de RTT.
 * - Cálculo de offset servidor -> cliente.
 * - Suavizado progresivo del offset.
 *
 * No depende del reloj de pared del dispositivo.
 */
class ClockSynchronizer {

    private val timeSource = TimeSource.Monotonic

    /**
     * Base monotónica utilizada por toda la sincronización.
     *
     * Es importante que el mismo origen temporal sea utilizado para
     * medir los tiempos locales y posteriormente convertir timestamps.
     */
    private val startMark = timeSource.markNow()

    /**
     * Offset estimado:
     *
     * serverTime = localTime + offset
     *
     * Por tanto:
     *
     * localTime = serverTime - offset
     */
    @Volatile
    private var serverOffsetMicros: Long = 0L

    /**
     * RTT estimado de la conexión.
     */
    @Volatile
    private var roundTripTimeMicros: Long = 0L

    /**
     * Indica si ya existe una estimación válida de sincronización.
     */
    @Volatile
    private var synchronized: Boolean = false

    /**
     * Número de muestras procesadas.
     */
    @Volatile
    private var sampleCount: Int = 0

    /**
     * Offset actual entre los relojes.
     */
    fun getOffsetMicros(): Long {
        return serverOffsetMicros
    }

    /**
     * RTT estimado en microsegundos.
     */
    fun getRoundTripTimeMicros(): Long {
        return roundTripTimeMicros
    }

    /**
     * Indica si el sincronizador tiene una estimación válida.
     */
    fun isSynchronized(): Boolean {
        return synchronized
    }

    /**
     * Devuelve el tiempo monotónico local en microsegundos.
     */
    fun localTimeMicros(): Long {
        return startMark.elapsedNow().inWholeMicroseconds
    }

    /**
     * Procesa una muestra de sincronización.
     *
     * Parámetros:
     *
     * t1 = momento en que Chiri envió la petición.
     * t2 = momento en que Music Assistant recibió la petición.
     * t3 = momento en que Music Assistant respondió.
     * t4 = momento en que Chiri recibió la respuesta.
     *
     * Todos los timestamps deben utilizar la misma unidad:
     * microsegundos.
     */
    @Synchronized
    fun update(
        t1LocalMicros: Long,
        t2ServerMicros: Long,
        t3ServerMicros: Long,
        t4LocalMicros: Long
    ) {
        if (t4LocalMicros < t1LocalMicros) {
            return
        }

        if (t3ServerMicros < t2ServerMicros) {
            return
        }

        val roundTrip =
            (t4LocalMicros - t1LocalMicros) -
                    (t3ServerMicros - t2ServerMicros)

        if (roundTrip < 0L) {
            return
        }

        /**
         * NTP-style offset:
         *
         * offset =
         * ((t2 - t1) + (t3 - t4)) / 2
         */
        val measuredOffset =
            (
                    (t2ServerMicros - t1LocalMicros) +
                            (t3ServerMicros - t4LocalMicros)
                    ) / 2L

        roundTripTimeMicros =
            if (sampleCount == 0) {
                roundTrip
            } else {
                smooth(
                    roundTripTimeMicros,
                    roundTrip
                )
            }

        serverOffsetMicros =
            if (sampleCount == 0) {
                measuredOffset
            } else {
                smooth(
                    serverOffsetMicros,
                    measuredOffset
                )
            }

        sampleCount++

        synchronized = true
    }

    /**
     * Convierte un timestamp del servidor a tiempo local monotónico.
     */
    fun serverTimeToLocalMicros(
        serverTimeMicros: Long
    ): Long {
        return serverTimeMicros - serverOffsetMicros
    }

    /**
     * Convierte tiempo local monotónico a tiempo del servidor.
     */
    fun localTimeToServerMicros(
        localTimeMicros: Long
    ): Long {
        return localTimeMicros + serverOffsetMicros
    }

    /**
     * Reinicia la estimación de sincronización.
     */
    @Synchronized
    fun reset() {
        serverOffsetMicros = 0L
        roundTripTimeMicros = 0L
        synchronized = false
        sampleCount = 0
    }

    /**
     * Suavizado exponencial.
     *
     * alpha = 0.1
     *
     * Mantiene estabilidad frente a pequeñas variaciones de red.
     */
    private fun smooth(
        previous: Long,
        current: Long
    ): Long {
        val alpha = 0.1

        return (
                previous +
                        ((current - previous) * alpha)
                ).toLong()
    }
}
