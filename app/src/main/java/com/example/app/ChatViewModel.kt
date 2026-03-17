package com.example.app

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.util.UUID

data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ip: String
)

data class Message(
    val author: String,
    val text: String,
    val isMine: Boolean
)

class ChatViewModel : ViewModel() {

    // Nome do usuário local
    var myName = mutableStateOf("Eu")

    // Lista de contatos
    var contacts = mutableStateListOf<Contact>()

    // Mensagens por contato (id -> lista)
    var messagesMap = mutableStateMapOf<String, MutableList<Message>>()

    // Contato selecionado atualmente
    var selectedContact = mutableStateOf<Contact?>(null)

    private var socket: Socket? = null

    fun addContact(name: String, ip: String) {
        val contact = Contact(name = name, ip = ip)
        contacts.add(contact)
        messagesMap[contact.id] = mutableStateListOf()
    }

    fun selectContact(contact: Contact) {
        socket?.disconnect()
        selectedContact.value = contact

        socket = IO.socket("http://${contact.ip}:3000")
        socket?.connect()

        socket?.on("receive_message") { args ->
            val data = args[0] as JSONObject
            val author = data.getString("author")
            val text = data.getString("message")
            val isMine = author == myName.value
            messagesMap[contact.id]?.add(Message(author, text, isMine))
        }
    }

    fun sendMessage(text: String) {
        val contact = selectedContact.value ?: return
        val json = JSONObject().apply {
            put("author", myName.value)
            put("message", text)
        }
        socket?.emit("send_message", json)
    }

    override fun onCleared() {
        super.onCleared()
        socket?.disconnect()
    }
}