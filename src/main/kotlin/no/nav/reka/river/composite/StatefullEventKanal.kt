package no.nav.reka.river.composite

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.reka.river.IDataFelt
import no.nav.reka.river.Key
import no.nav.reka.river.MessageType
import no.nav.reka.river.demandValue
import no.nav.reka.river.model.Event
import no.nav.reka.river.redis.IRedisStore
import org.slf4j.LoggerFactory
import java.util.UUID

class StatefullEventKanal(
    val eventName: MessageType.Event,
    val redisStore: IRedisStore,
    private val dataFelter: Array<IDataFelt>,
    override val mainListener: MessageListener,
) : DelegatingEventListener(
    mainListener
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun accept(): River.PacketValidation {
        return River.PacketValidation {
            it.demandValue(Key.EVENT_NAME,eventName)
            it.interestedIn(*dataFelter.map { it.str }.toTypedArray())
        }
    }

    private fun collectData(packet: Event) {
        val transactionId = UUID.randomUUID().toString()
        packet.uuid = transactionId

        dataFelter.map { dataFelt ->
            Pair(dataFelt, packet[dataFelt])
        }.forEach { data ->
            val str = if ((data.second as JsonNode).isTextual) { data.second?.asText()?:"" } else data.second.toString()
            redisStore.set(transactionId + data.first, str)
        }
    }
    override fun onEvent(packet: Event) {
        log.info("Statefull event listener for event ${packet.event.value}" + " med paket  ${packet.toString()}")
        collectData(packet)
        mainListener.onMessage(packet)
    }
}
