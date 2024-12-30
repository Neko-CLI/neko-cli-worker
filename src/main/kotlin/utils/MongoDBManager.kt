@file:Suppress("UselessCallOnNotNull", "SpellCheckingInspection")

package utils

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MongoDBManager {

    private val api = NekoCLIApi()
    private lateinit var client: MongoClient
    private lateinit var database: MongoDatabase
    private val logger: Logger = LoggerFactory.getLogger(MongoDBManager::class.java)
    private val connectionString = "mongodb://${api.getConfig("USERNAME")}:${api.getConfig("MONGODBPASSWORD")}@${api.getConfig("HOST")}:${api.getConfig("PORT")}/neko_cli_worker"

    fun connect() {
        try {
            val connectionString = connectionString
            client = MongoClients.create(connectionString)
            database = client.getDatabase("neko_cli_worker")
            println("Connected to MongoDB.")
        } catch (e: Exception) {
            println("Error connecting to MongoDB: ${e.message}")
        }
    }

    fun getWarningsCollection(): MongoCollection<Document> {
        return database.getCollection("warnings")
    }

    fun getBansCollection(): MongoCollection<Document> {
        return database.getCollection("bans")
    }

    fun insertWarning(document: Document) {
        getWarningsCollection().insertOne(document)
        logger.info("Inserted document into warnings: $document")
    }

    fun insertBans(document: Document) {
        getBansCollection().insertOne(document)
        logger.info("Inserted document into bans: $document")
    }

    fun findWarnings(filter: Document): List<Document> {
        return getWarningsCollection().find(filter).toList()
    }

    fun findBans(filter: Document): List<Document> {
        return getBansCollection().find(filter).toList()
    }

    fun deleteWarnings(filter: Document) {
        val result = getWarningsCollection().deleteMany(filter)
        logger.info("Deleted ${result.deletedCount} documents from warnings")
    }

    fun deleteBans(filter: Document) {
        val result = getBansCollection().deleteMany(filter)
        logger.info("Deleted ${result.deletedCount} documents from bans")
    }
}
