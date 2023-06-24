package no.nav.reka.river.examples.`2stepcomposite`

import no.nav.reka.river.IDataFelt
import no.nav.reka.river.MessageType

enum class Events(name: String)  : MessageType.Event{
    APPLICATION_INITIATED("registration-started"),
    APPLICATION_RECIEVED("registration-accepted")

}


enum class Behov(name: String) :MessageType.Behov {
    FULL_NAME("full-name"),
    ADDRESS("address"),
    PERSIST_APPLICATION("persist-application")
}

enum class DataFelt(override val str: String) : IDataFelt {
        NAME("full-name"),
        ADDRESS("address"),
        APPLICATION_ID("application-id")
}