package no.nav.reka.river.examples.dsltest.services

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.reka.river.*
import no.nav.reka.river.examples.example_1_basic_løser.BehovName
import no.nav.reka.river.examples.example_1_basic_løser.DataFelt
import no.nav.reka.river.model.Behov
import no.nav.reka.river.model.Event
import no.nav.reka.river.model.Fail

class DocumentRecievedListener(rapidsConnection: RapidsConnection, val rapid: Rapid = Rapid(rapidsConnection) ) : IEventListener, IFailListener {


    override fun onEvent(packet: Event) {
        this.rapid.publishBehov(packet.createBehov(BehovName.FORMAT_DOCUMENT, mapOf(DataFelt.RAW_DOCUMENT to packet[DataFelt.RAW_DOCUMENT],
            DataFelt.RAW_DOCUMENT_FORMAT to "ebcdic")))
    }

    override fun onFail(fail: Fail) {

        this.rapid.publishBehov(Behov.create(fail.event,BehovName.FORMAT_DOCUMENT_IBM,mapOf(DataFelt.RAW_DOCUMENT to fail[DataFelt.RAW_DOCUMENT])))
    }

}