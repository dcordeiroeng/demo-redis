@Service
class ConsumerKafka(private val processMessageServiceUseCase: ProcessMessageServiceUseCase) : Loggable {

    @KafkaListener(
        topics = ["\${'$'}{topic.name}"],
        groupId = "\${'$'}{spring.kafka.consumer.group-id}",
        clientIdPrefix = "\${'$'}{spring.kafka.consumer.client-id}"
    )
    fun receiveMessage(@Payload message: String, acknowledgment: Acknowledgment) {
        runCatching {
            logger.info("Received message: {}", message)
            processMessageServiceUseCase.execute(message)
            logger.info("Message processed successfully")
        }.onFailure {
            logger.error("Error processing message: {}", it.message)
        }.also {
            acknowledgment.acknowledge()
        }
    }
}

fun execute(message: String) {
    val input = UtilsGeral.StringToInputStream(message)
    val cardData = convertMessage(input)
    processMessage(cardData, input)
}

private fun processMessage(event: CardData, input: InputStreamReader) {
    var processingError: Throwable? = null

    when (event.eventType()) {
        EventOffload.DELETE_DOCUMENT_NUMBER -> {
            logger.info("Processing delete event")
            runCatching {
                deleteDocumentNumber(event)
            }.onFailure {
                logger.error("Failed to delete document number: {}", it.message)
                processingError = it
            }
        }

        EventOffload.EXPIRATION_DATE -> {
            logger.info("Processing update for expiration date event")
            runCatching {
                updateExpirationDate(event, input)
            }.onFailure {
                logger.error("Failed to update expiration date: {}", it.message)
                processingError = it
            }
        }

        else -> logger.info("Event {} not found", event.eventType())
    }

    runCatching {
        createCardHolder(event, input)
    }.onFailure {
        logger.error("Error in createCardHolder - Event: ${'$'}{event.input}, Message: ${'$'}{it.message}")
        processingError = processingError ?: it
    }

    processingError?.let {
        throw it
    }
}

private fun convertMessage(input: InputStreamReader): CardData {
    return runCatching {
        TransactionCode.fromTrancode(input, CardData::class.java).also {
            logger.info("Message converted")
        }
    }.onFailure {
        logger.error("Error converting message: {}", it.message)
    }.getOrThrow()
}

private fun createCardHolder(event: CardData, input: InputStreamReader) {
    if (!offloadCardHolderEnabled) return
    createCardHolderUseCase.execute(event.input)
}

private fun deleteDocumentNumber(event: CardData) {
    genericErrorFlowPort.removeDocumentNumber(event.documentNumber!!)
}

private fun updateExpirationDate(event: CardData, input: InputStreamReader) {
    val expirationDate = TransactionCode.fromTransactionCode(
        input.changeExpirationDateDTO?.transactionCode
    ).expirationDateDTO()

    genericErrorFlowPort.updateDataTransfer(
        event.cardId!!,
        expirationDate.toUpdateTransferData()
    )
}

override fun removeDocumentNumber(documentNumber: String) {
    executeWithFeignHandling(
        errorContext = "remove document number in GenericCardListOffload",
        operation = {
            val token = authService.generateStsAccessToken()
            val documentValidated = documentNumber.validateDocumentNumber()

            logger.info("Calling Offload API to delete document number ${'$'}documentNumber")
            genericListOffloadClient.removeDocumentNumber(
                authorization = token.accessToken,
                flowId = UUID.randomUUID().toString(),
                cid = UUID.randomUUID().toString(),
                documentNumber = documentNumber,
                personType = documentValidated.toDefinePersonType()
            )
        }
    )
}

private fun executeWithFeignHandling(operation: () -> Unit, errorContext: String) {
    try {
        operation()
        logger.info("Operation '${'$'}errorContext' completed successfully.")
    } catch (error: Throwable) {
        when (error) {
            is FeignException -> {
                logger.error(
                    "FeignException when trying to '${'$'}errorContext'. Status: ${'$'}{error.status()}. Content: ${'$'}{error.contentUTF8()}",
                    error
                )

                val message = when (error.status()) {
                    HttpStatus.BAD_REQUEST.value() -> "Bad request when trying to connect to GenericCardListOffload"
                    HttpStatus.SERVICE_UNAVAILABLE.value() -> "GenericCardListOffload service is unavailable"
                    HttpStatus.INTERNAL_SERVER_ERROR.value() -> "GenericCardListOffload responded with an internal error"
                    HttpStatus.NOT_FOUND.value() -> "Not found in GenericCardListOffload"
                    HttpStatus.FORBIDDEN.value() -> "Access forbidden to GenericCardListOffload"
                    else -> "Unexpected Feign error occurred with status code ${'$'}{error.status()} when trying to '${'$'}errorContext'"
                }
                throw RuntimeException(message, error)
            }

            else -> {
                val message = "Unexpected error when trying to '${'$'}errorContext': ${'$'}{error.message}"
                logger.error(message, error)
                throw RuntimeException(message, error)
            }
        }
    }
}
