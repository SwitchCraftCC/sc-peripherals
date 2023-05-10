package io.sc3.peripherals

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.HTTPServer
import io.sc3.peripherals.config.ScPeripheralsConfig
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

object ScPeripheralsPrometheus {
  private val log = LoggerFactory.getLogger(ScPeripheralsPrometheus::class.java)

  private var prometheusServer: HTTPServer? = null
  internal val registry = CollectorRegistry(true)

  fun init() {
    ServerLifecycleEvents.SERVER_STARTING.register {
      if (ScPeripheralsConfig.config["prometheus.enabled"]) {
        log.info("Starting Prometheus server on port ${ScPeripheralsConfig.config.get<Int>("prometheus.port")}")
        prometheusServer = HTTPServer(InetSocketAddress(ScPeripheralsConfig.config["prometheus.port"]), registry, true)
      }
    }

    ServerLifecycleEvents.SERVER_STOPPING.register {
      try {
        log.info("Stopping Prometheus server")
        prometheusServer?.close()?.also { prometheusServer = null }
      } catch (e: Exception) {
        log.error("Failed to stop Prometheus server", e)
      }
    }
  }
}
