package no.nav.reka.river.composite

import no.nav.helsearbeidsgiver.felles.rapidsrivers.composite.Transaction
import no.nav.reka.river.basic.EventListener
import no.nav.reka.river.MessageType
import no.nav.reka.river.model.Event
import no.nav.reka.river.model.Fail
import no.nav.reka.river.model.Message
import no.nav.reka.river.model.TxMessage
import no.nav.reka.river.redis.IRedisStore
import no.nav.reka.river.redis.RedisKey
import org.slf4j.LoggerFactory

abstract class Saga(open val redisStore: IRedisStore) : MessageListener {
    private val log = LoggerFactory.getLogger(this::class.java)
    abstract val event: MessageType.Event
    private lateinit var dataKanal: StatefullDataKanal

    override fun onMessage(packet: Message) {
        val txMessage = packet as TxMessage
        val transaction: Transaction = determineTransactionState(txMessage)

        when (transaction) {
            Transaction.NEW -> {
                initialTransactionState(txMessage)
                dispatchBehov(txMessage, transaction)
            }
            Transaction.IN_PROGRESS -> dispatchBehov(txMessage, transaction)
            Transaction.FINALIZE -> finalize(txMessage)
            Transaction.TERMINATE -> terminate(txMessage)
            Transaction.NOT_ACTIVE -> notActive(txMessage)
        }
    }

    private fun notActive(message: TxMessage) {
        log.error("Transaction is not active for message $message")
    }

    fun determineTransactionState(message: TxMessage): Transaction {
        // event bør ikke ha UUID men dette er ikke konsistent akkuratt nå så midlertidig blir det sånn til vi får det konsistent.
        // vi trenger også clientID for correlation
        val transactionId = message.uuid()!!
        if (message is Fail) { // Returnerer INPROGRESS eller TERMINATE
            log.error("Feilmelding er ${message.toString()}")
            return onError(message as Fail)
        }

        val eventKey = RedisKey.transactionKey(transactionId, event)
        val value = redisStore.get(eventKey)
        if (value.isNullOrEmpty()) {
            if (!(message is Event)) {
                log.error("TransactionID can be undefined only if the incoming message is Event.")
                return Transaction.NOT_ACTIVE
            }

            redisStore.set(eventKey, message.clientId?:transactionId)
            return Transaction.NEW
        } else {
            if (isDataCollected(transactionId)) return Transaction.FINALIZE
        }
        return Transaction.IN_PROGRESS
    }


    abstract fun dispatchBehov(message: TxMessage, transaction: Transaction)
    abstract fun finalize(message: TxMessage)
    abstract fun terminate(message: TxMessage)
    open fun initialTransactionState(message: TxMessage) {}

    open fun onError(feil: Fail): Transaction {
        return Transaction.TERMINATE
    }

    fun withFailKanal(failKanalSupplier: (t: Saga) -> FailKanal): Saga {
        failKanalSupplier.invoke(this)
        return this
    }

    fun withEventListener(eventListenerSupplier: (t: Saga) -> EventListener): Saga {
        eventListenerSupplier.invoke(this)
        return this
    }

    fun withDataKanal(dataKanalSupplier: (t: Saga) -> StatefullDataKanal): Saga {
        dataKanal = dataKanalSupplier.invoke(this)
        return this
    }

    open fun isDataCollected(uuid: String): Boolean = dataKanal.isAllDataCollected(RedisKey.clientKey(uuid))
    open fun isDataCollected(vararg keys: RedisKey): Boolean = dataKanal.isDataCollected(*keys)
}
